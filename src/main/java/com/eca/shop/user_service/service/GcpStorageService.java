package com.eca.shop.user_service.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
public class GcpStorageService {

    // Define the GCP bucket name for user profiles
    private final String BUCKET_NAME = "eca-user-profiles-bucket";

    // The Storage object to interact with Google Cloud Storage
    private final Storage storage;

    // Constructor to initialize the GCP Storage client using the credentials JSON file
    public GcpStorageService() throws IOException {
        GoogleCredentials credentials = GoogleCredentials.fromStream(
                new ClassPathResource("gcp-credentials.json").getInputStream());
        this.storage = StorageOptions.newBuilder().setCredentials(credentials).build().getService();
    }

    /**
     * Uploads the profile image to Google Cloud Storage and returns the public URL.
     *
     * @param profileImage The multipart file received from the client
     * @return The public URL of the uploaded image
     * @throws IOException If an error occurs during file processing
     */
    public String uploadProfileImage(MultipartFile profileImage) throws IOException {

        // Generate a unique file name
        String fileName = UUID.randomUUID().toString() + "-" + System.currentTimeMillis() + ".jpg";

        // Create an output stream to hold the compressed image data
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // Compress the image using Thumbnailator
        // Resizes to 800x800 maximum, sets format to JPG, and reduces quality to 70%
        Thumbnails.of(profileImage.getInputStream())
                .size(800, 800)
                .outputFormat("jpg")
                .outputQuality(0.7)
                .toOutputStream(outputStream);

        // Convert the compressed image back to an InputStream for GCP upload
        byte[] compressedImageBytes = outputStream.toByteArray();
        InputStream compressedInputStream = new ByteArrayInputStream(compressedImageBytes);

        // Build the BlobInfo for GCP
        BlobId blobId = BlobId.of(BUCKET_NAME, fileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType("image/jpeg") // Always setting as JPEG due to outputFormat("jpg")
                .build();

        // Upload the compressed image stream to GCP
        storage.createFrom(blobInfo, compressedInputStream);

        // Return the public URL
        return "https://storage.googleapis.com/" + BUCKET_NAME + "/" + fileName;
    }
}
package com.eca.shop.user_service.service;

import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class GcpStorageService {

    // Define the GCP bucket name for user profiles
    private final String BUCKET_NAME = "eca-user-profiles-bucket";

    /**
     * Uploads the profile image to Google Cloud Storage and returns the public URL.
     *
     * @param file The multipart file received from the client
     * @return The public URL of the uploaded image
     * @throws IOException If an error occurs during file processing
     */
    public String uploadProfileImage(MultipartFile file) throws IOException {
        // Initialize the GCP Storage service
        Storage storage = StorageOptions.getDefaultInstance().getService();

        // Generate a unique file name to prevent overriding existing files
        String fileName = UUID.randomUUID().toString() + "-" + file.getOriginalFilename();

        // Configure the blob (file) details including the target bucket and content type
        BlobInfo blobInfo = BlobInfo.newBuilder(BUCKET_NAME, fileName)
                .setContentType(file.getContentType())
                .build();

        // Upload the file bytes to GCP Storage
        storage.create(blobInfo, file.getBytes());

        // Construct and return the public URL to access the uploaded image
        return String.format("https://storage.googleapis.com/%s/%s", BUCKET_NAME, fileName);
    }
}
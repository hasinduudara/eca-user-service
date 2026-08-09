package com.eca.shop.user_service.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.core.io.ClassPathResource;
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
        // Read json file for resources
        ClassPathResource resource = new ClassPathResource("gcp-credentials.json");

        // Connect Storage using Credentials
        Storage storage = StorageOptions.newBuilder()
                .setCredentials(GoogleCredentials.fromStream(resource.getInputStream()))
                .build()
                .getService();

        String fileName = UUID.randomUUID().toString() + "-" + file.getOriginalFilename();

        BlobInfo blobInfo = BlobInfo.newBuilder(BUCKET_NAME, fileName)
                .setContentType(file.getContentType())
                .build();

        storage.create(blobInfo, file.getBytes());

        return String.format("https://storage.googleapis.com/%s/%s", BUCKET_NAME, fileName);
    }
}
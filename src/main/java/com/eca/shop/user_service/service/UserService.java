package com.eca.shop.user_service.service;

import com.eca.shop.user_service.entity.User;
import com.eca.shop.user_service.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GcpStorageService gcpStorageService;

    /**
     * Registers a new user in the database. Uploads the profile image if provided.
     *
     * @param name         The name of the user
     * @param email        The email of the user
     * @param password     The password of the user
     * @param profileImage The profile image file (optional)
     * @return The saved User entity
     * @throws IOException If image upload fails
     */
    public User registerUser(String name, String email, String password, MultipartFile profileImage) throws IOException {

        String imageUrl = null;

        // Check if a profile image was provided and upload it to GCP
        if (profileImage != null && !profileImage.isEmpty()) {
            imageUrl = gcpStorageService.uploadProfileImage(profileImage);
        }

        // Build the User object using Lombok builder
        // Note: In a production environment, the password must be encrypted using BCrypt
        User user = User.builder()
                .name(name)
                .email(email)
                .password(password)
                .profileImageUrl(imageUrl)
                .build();

        // Save the user entity to the MySQL database
        return userRepository.save(user);
    }

    /**
     * Retrieves a user by their ID.
     *
     * @param id The ID of the user
     * @return The User entity if found
     */
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));
    }
}
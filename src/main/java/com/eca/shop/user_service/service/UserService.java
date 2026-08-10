package com.eca.shop.user_service.service;

import com.eca.shop.user_service.entity.User;
import com.eca.shop.user_service.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GcpStorageService gcpStorageService;

    // Injecting the PasswordEncoder to hash passwords
    @Autowired
    private PasswordEncoder passwordEncoder;

    // Inject JwtService for token generation
    @Autowired
    private JwtService jwtService;

    // Inject EmailService to send emails
    @Autowired
    private com.eca.shop.user_service.service.EmailService emailService;

    /**
     * Registers a new user in the database. Uploads the profile image if provided.
     * The raw password is encrypted using BCrypt before saving.
     *
     * @param name         The name of the user
     * @param email        The email of the user
     * @param password     The raw password of the user
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

        // Encrypt the raw password using BCryptPasswordEncoder
        String encryptedPassword = passwordEncoder.encode(password);

        // Build the User object using Lombok builder with the encrypted password
        User user = User.builder()
                .name(name)
                .email(email)
                .password(encryptedPassword) // Set the encrypted password here
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

    /**
     * Updates an existing user's details and profile image.
     *
     * @param id           The ID of the user to update
     * @param name         The new name (optional)
     * @param email        The new email (optional)
     * @param profileImage The new profile image file (optional)
     * @return The updated User entity
     * @throws IOException If image upload fails
     */
    public User updateUser(Long id, String name, String email, MultipartFile profileImage) throws IOException {

        // Find the existing user from the database
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));

        // Update the name if provided
        if (name != null && !name.trim().isEmpty()) {
            existingUser.setName(name);
        }

        // Update the email if provided
        if (email != null && !email.trim().isEmpty()) {
            existingUser.setEmail(email);
        }

        // Check if a new profile image was provided, upload it, and update the URL
        if (profileImage != null && !profileImage.isEmpty()) {
            String newImageUrl = gcpStorageService.uploadProfileImage(profileImage);
            existingUser.setProfileImageUrl(newImageUrl);
        }

        // Save and return the updated user entity
        return userRepository.save(existingUser);
    }

    /**
     * Authenticates a user and generates JWT tokens.
     *
     * @param email    The user's email
     * @param password The raw password provided by the user
     * @return AuthResponse containing access and refresh tokens
     */
    public com.eca.shop.user_service.dto.AuthResponse loginUser(String email, String password) {

        // Find the user by email
        // Note: You need to add findByEmail(String email) method in UserRepository if not already present
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        // Check if the provided password matches the encrypted password in the database
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        // Generate tokens if authentication is successful
        String accessToken = jwtService.generateAccessToken(user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());

        // Return the tokens
        return com.eca.shop.user_service.dto.AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    /**
     * Generates a 6-digit OTP, saves it with an expiration time, and sends it via email.
     *
     * @param email The user's email address
     */
    public void processForgotPassword(String email) {
        // Find the user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        // Generate a random 6-digit OTP
        String otp = String.format("%06d", new java.util.Random().nextInt(999999));

        // Set the OTP and its expiration time (e.g., 10 minutes from now)
        user.setResetOtp(otp);
        user.setResetOtpExpiryTime(java.time.LocalDateTime.now().plusMinutes(10));

        // Save the updated user to the database
        userRepository.save(user);

        // Prepare and send the email
        String emailSubject = "Password Reset OTP - ECA Shop";
        String emailBody = "Your password reset OTP is: " + otp + "\n\nThis OTP is valid for 10 minutes. Do not share this with anyone.";
        emailService.sendSimpleEmail(user.getEmail(), emailSubject, emailBody);
    }

    /**
     * Validates the OTP and updates the user's password.
     *
     * @param email       The user's email address
     * @param otp         The OTP received by the user
     * @param newPassword The new password
     */
    public void resetPassword(String email, String otp, String newPassword) {
        // Find the user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        // Check if the OTP matches
        if (user.getResetOtp() == null || !user.getResetOtp().equals(otp)) {
            throw new RuntimeException("Invalid OTP");
        }

        // Check if the OTP has expired
        if (user.getResetOtpExpiryTime().isBefore(java.time.LocalDateTime.now())) {
            throw new RuntimeException("OTP has expired");
        }

        // Encrypt the new password and update the user entity
        user.setPassword(passwordEncoder.encode(newPassword));

        // Clear the OTP fields so it cannot be reused
        user.setResetOtp(null);
        user.setResetOtpExpiryTime(null);

        // Save the updated user to the database
        userRepository.save(user);
    }

    /**
     * Generates a new Access Token using a valid Refresh Token.
     *
     * @param refreshToken The valid refresh token provided by the user
     * @return AuthResponse containing the new access token and the same refresh token
     */
    public com.eca.shop.user_service.dto.AuthResponse refreshToken(String refreshToken) {
        // Validate the refresh token
        if (jwtService.isTokenValid(refreshToken)) {

            // Extract the user's email from the token
            String email = jwtService.extractEmail(refreshToken);

            // Generate a new access token
            String newAccessToken = jwtService.generateAccessToken(email);

            // Return the new tokens (keeping the old refresh token valid)
            return com.eca.shop.user_service.dto.AuthResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(refreshToken)
                    .build();
        } else {
            throw new RuntimeException("Invalid or expired refresh token");
        }
    }
}
package com.eca.shop.user_service.controller;

import com.eca.shop.user_service.entity.User;
import com.eca.shop.user_service.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * REST endpoint to register a new user.
     * Accepts multipart/form-data to support file uploads.
     */
    @PostMapping("/register")
    public ResponseEntity<User> registerUser(
            @RequestParam("name") String name,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam(value = "profileImage", required = false) MultipartFile profileImage) {

        try {
            // Call the service layer to handle business logic and persistence
            User savedUser = userService.registerUser(name, email, password, profileImage);
            return ResponseEntity.ok(savedUser);
        } catch (Exception e) {
            // Return a 400 Bad Request status if any exception occurs (e.g., upload failure)
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * REST endpoint to fetch user profile details by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserProfile(@PathVariable Long id) {
        try {
            // Fetch the user from the database
            User user = userService.getUserById(id);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            // Return a 404 Not Found status if the user does not exist
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Endpoint to update user details and profile image.
     *
     * @param id           The ID of the user
     * @param name         The new name (optional)
     * @param email        The new email (optional)
     * @param profileImage The new profile image (optional)
     * @return ResponseEntity containing the updated User or error status
     */
    @PutMapping("/update/{id}")
    public ResponseEntity<User> updateUser(
            @PathVariable Long id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) MultipartFile profileImage) {

        try {
            User updatedUser = userService.updateUser(id, name, email, profileImage);
            return ResponseEntity.ok(updatedUser);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
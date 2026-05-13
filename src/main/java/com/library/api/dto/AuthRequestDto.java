package com.library.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

// Data sent from the client when logging in or registering
// Validation annotations reject invalid data before it reaches the service layer
@Data
public class AuthRequestDto {

    // Username is required and must be between 3 and 50 characters
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    // Password is required and must be at least 8 characters for security
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
}

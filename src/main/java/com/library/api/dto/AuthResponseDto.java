package com.library.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

// Response sent to the client after a successful login or registration
// The client must store the token and include it in all future requests
@Data
@AllArgsConstructor
public class AuthResponseDto {

    // JWT token — sent in the Authorization header as: Bearer <token>
    private String token;

    private String username;

    // Role helps the frontend decide which UI elements to show or hide
    private String role;
}

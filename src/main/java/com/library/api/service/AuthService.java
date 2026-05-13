package com.library.api.service;

import com.library.api.dto.*;
import com.library.api.entity.AppUser;
import com.library.api.repository.UserRepository;
import com.library.api.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

// Handles user registration and login logic
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    // Register a new account with the ROLE_USER role by default
    public AuthResponseDto register(AuthRequestDto request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already taken: " + request.getUsername());
        }

        // Hash the password before saving — never store plain-text passwords
        AppUser newUser = AppUser.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role("ROLE_USER")
                .build();

        userRepository.save(newUser);

        // Return a token immediately so the user is logged in right after registering
        String token = jwtUtil.generateToken(newUser.getUsername(), newUser.getRole());
        return new AuthResponseDto(token, newUser.getUsername(), newUser.getRole());
    }

    // Verify credentials and return a JWT token on success
    public AuthResponseDto login(AuthRequestDto request) {
        // AuthenticationManager automatically throws BadCredentialsException on wrong credentials
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        var user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found after authentication"));

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
        return new AuthResponseDto(token, user.getUsername(), user.getRole());
    }
}

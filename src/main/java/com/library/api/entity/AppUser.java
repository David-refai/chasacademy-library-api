package com.library.api.entity;

import jakarta.persistence.*;
import lombok.*;

// Represents a user stored in the USERS table
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Username must be unique across the entire database
    @Column(unique = true, nullable = false)
    private String username;

    // Password stored as a BCrypt hash — never store plain-text passwords
    @Column(nullable = false)
    private String password;

    // ROLE_USER for regular users, ROLE_ADMIN for administrators
    @Column(nullable = false)
    private String role;
}

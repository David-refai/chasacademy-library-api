package com.library.api.config;

import com.library.api.security.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;

import java.util.List;

// Main security configuration for the application
// Defines which endpoints are public, which require authentication, and how JWT is integrated
@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // Enables @PreAuthorize annotations in controllers
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // CSRF is disabled because stateless REST APIs use JWT, not cookies
                .csrf(csrf -> csrf.disable())

                // Apply the CORS policy defined in corsConfigurationSource()
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Define access rules per endpoint
                .authorizeHttpRequests(auth -> auth
                        // Login and registration are open to everyone
                        .requestMatchers("/api/auth/**").permitAll()
                        // H2 console is open in development only
                        .requestMatchers("/h2-console/**").permitAll()
                        // Health check endpoint is public for monitoring tools
                        .requestMatchers("/actuator/health").permitAll()
                        // Every other endpoint requires a valid JWT token
                        .anyRequest().authenticated()
                )

                // Stateless sessions — each request must carry its own JWT token
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Run JwtFilter before Spring's default username/password filter
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)

                // Allow H2 console iframes (disable in production)
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))

                .build();
    }

    // CORS configuration — controls which origins, methods, and headers are allowed
    // Prevents malicious websites from sending requests on behalf of the user
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var config = new CorsConfiguration();

        // Only allow requests from these specific origins (replace with production URL)
        config.setAllowedOrigins(List.of(
                "http://localhost:3000",    // React frontend
                "http://localhost:4200"     // Angular frontend
        ));

        // Restrict to these HTTP methods only
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // Allow the Authorization header so clients can send the JWT token
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));

        // Allow credentials (cookies) in cross-origin requests
        config.setAllowCredentials(true);

        // Browser can cache the CORS preflight response for 1 hour
        config.setMaxAge(3600L);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    // BCrypt with cost factor 12 — strong security with acceptable performance
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    // Used by AuthService to verify login credentials
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }
}

package com.library.api;

import com.library.api.dto.AuthRequestDto;
import com.library.api.dto.AuthResponseDto;
import com.library.api.entity.AppUser;
import com.library.api.repository.UserRepository;
import com.library.api.security.JwtUtil;
import com.library.api.service.AuthService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Unit tests for AuthService — covers registration and login logic
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    // ===== REGISTER TESTS =====

    @Test
    @DisplayName("register: should create user and return token on valid input")
    void register_shouldCreateUserAndReturnToken_whenValid() {
        AuthRequestDto request = new AuthRequestDto();
        request.setUsername("david");
        request.setPassword("password123");

        // Username is not taken
        when(userRepository.existsByUsername("david")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$hashed");
        when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtUtil.generateToken("david", "ROLE_USER")).thenReturn("mocked-jwt-token");

        AuthResponseDto result = authService.register(request);

        assertThat(result.getToken()).isEqualTo("mocked-jwt-token");
        assertThat(result.getUsername()).isEqualTo("david");
        assertThat(result.getRole()).isEqualTo("ROLE_USER");

        // Verify the password was hashed before saving
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(AppUser.class));
    }

    @Test
    @DisplayName("register: should throw IllegalArgumentException when username is already taken")
    void register_shouldThrow_whenUsernameAlreadyExists() {
        AuthRequestDto request = new AuthRequestDto();
        request.setUsername("david");
        request.setPassword("password123");

        when(userRepository.existsByUsername("david")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already taken");

        // Confirm we never tried to save anything
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register: should never store a plain-text password")
    void register_shouldEncodePassword_beforeSaving() {
        AuthRequestDto request = new AuthRequestDto();
        request.setUsername("newuser");
        request.setPassword("plaintext");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("plaintext")).thenReturn("$2a$hashed");
        when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> {
            AppUser saved = inv.getArgument(0);
            // The stored password must NOT be the plain-text version
            assertThat(saved.getPassword()).isNotEqualTo("plaintext");
            return saved;
        });
        when(jwtUtil.generateToken(any(), any())).thenReturn("token");

        authService.register(request);

        verify(passwordEncoder).encode("plaintext");
    }

    // ===== LOGIN TESTS =====

    @Test
    @DisplayName("login: should return token on correct credentials")
    void login_shouldReturnToken_whenCredentialsAreCorrect() {
        AuthRequestDto request = new AuthRequestDto();
        request.setUsername("admin");
        request.setPassword("admin1234");

        AppUser user = AppUser.builder()
                .id(1L)
                .username("admin")
                .password("$2a$hashed")
                .role("ROLE_ADMIN")
                .build();

        // AuthenticationManager succeeds (no exception thrown)
        when(authenticationManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken("admin", "admin1234")
        );
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken("admin", "ROLE_ADMIN")).thenReturn("admin-jwt-token");

        AuthResponseDto result = authService.login(request);

        assertThat(result.getToken()).isEqualTo("admin-jwt-token");
        assertThat(result.getRole()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    @DisplayName("login: should throw BadCredentialsException on wrong password")
    void login_shouldThrow_whenPasswordIsWrong() {
        AuthRequestDto request = new AuthRequestDto();
        request.setUsername("admin");
        request.setPassword("wrongpassword");

        // AuthenticationManager throws when credentials are wrong
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);

        // User lookup should never happen if authentication failed
        verify(userRepository, never()).findByUsername(any());
    }
}

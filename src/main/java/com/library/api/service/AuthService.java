package com.library.api.service;

import com.library.api.dto.*;
import com.library.api.entity.AppUser;
import com.library.api.repository.UserRepository;
import com.library.api.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

// منطق تسجيل الدخول وإنشاء الحسابات
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    // إنشاء حساب جديد بدور مستخدم عادي
    public AuthResponseDto register(AuthRequestDto request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already taken: " + request.getUsername());
        }

        // نُشفّر كلمة المرور قبل الحفظ - لا نحفظ كلمات المرور كنص عادي أبداً
        AppUser newUser = AppUser.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role("ROLE_USER")
                .build();

        userRepository.save(newUser);

        // نُعيد token مباشرةً حتى يتمكن المستخدم من الاستخدام فور التسجيل
        String token = jwtUtil.generateToken(newUser.getUsername(), newUser.getRole());
        return new AuthResponseDto(token, newUser.getUsername(), newUser.getRole());
    }

    // التحقق من بيانات الدخول وإعادة JWT token
    public AuthResponseDto login(AuthRequestDto request) {
        // AuthenticationManager يتحقق من اسم المستخدم وكلمة المرور تلقائياً
        // يرمي BadCredentialsException تلقائياً إذا كانت البيانات خاطئة
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        var user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found after authentication"));

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
        return new AuthResponseDto(token, user.getUsername(), user.getRole());
    }
}

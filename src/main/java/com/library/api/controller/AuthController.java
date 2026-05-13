package com.library.api.controller;

import com.library.api.dto.*;
import com.library.api.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

// نقطة دخول طلبات تسجيل الدخول وإنشاء الحسابات
// @Valid تُفعّل التحقق من صحة البيانات المُعرَّف في AuthRequestDto
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // POST /api/auth/register
    // مثال للطلب: {"username": "ahmed", "password": "password123"}
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody AuthRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    // POST /api/auth/login
    // يُعيد JWT token يجب وضعه في header: Authorization: Bearer <token>
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody AuthRequestDto request) {
        return ResponseEntity.ok(authService.login(request));
    }
}

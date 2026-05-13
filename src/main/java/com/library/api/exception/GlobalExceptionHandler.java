package com.library.api.exception;

import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

// معالج مركزي لكل الأخطاء في التطبيق
// بدونه، Spring يُعيد أشكالاً مختلفة من رسائل الخطأ حسب نوع الاستثناء
// معه، كل الأخطاء تأتي بنفس الشكل الموحد مما يُسهّل عمل الواجهة الأمامية
@RestControllerAdvice
public class GlobalExceptionHandler {

    // معالجة أخطاء التحقق من الإدخال (@Valid فشل)
    // مثال: أرسل المستخدم ISBN من 5 أرقام بدلاً من 13
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        // نجمع كل أخطاء الحقول في map واضحة
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        return ResponseEntity.badRequest().body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", 400,
                "error", "Validation Failed",
                "details", fieldErrors
        ));
    }

    // معالجة "مورد غير موجود" مثل كتاب بـ ID غير موجود
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", 404,
                "error", ex.getMessage()
        ));
    }

    // معالجة أخطاء المنطق مثل: تسجيل بـ username مستخدم أو ISBN مكرر
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", 400,
                "error", ex.getMessage()
        ));
    }

    // معالجة أخطاء الحالة مثل: محاولة استعارة كتاب مُستعار أو إعادة كتاب مُعاد
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", 409,
                "error", ex.getMessage()
        ));
    }

    // معالجة محاولة الوصول بدون صلاحية مثل: مستخدم عادي يحاول إضافة كتاب
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", 403,
                "error", "You do not have permission to perform this action"
        ));
    }

    // معالجة بيانات تسجيل دخول خاطئة
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", 401,
                "error", "Invalid username or password"
        ));
    }
}

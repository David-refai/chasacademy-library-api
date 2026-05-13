package com.library.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

// البيانات المُرسَلة من العميل عند تسجيل الدخول أو إنشاء حساب
// الـ annotations تمنع Spring من قبول بيانات غير صالحة قبل الوصول للـ service
@Data
public class AuthRequestDto {

    // اسم المستخدم مطلوب وطوله بين 3 و 50 حرفاً
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    // كلمة المرور مطلوبة وطولها 8 أحرف على الأقل لضمان الأمان
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
}

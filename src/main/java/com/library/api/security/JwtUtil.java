package com.library.api.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

// أداة إنشاء والتحقق من JWT tokens
// JWT = JSON Web Token - وسيلة آمنة لحمل معلومات المستخدم بين الطلبات
// الـ token مُوقَّع رقمياً فلا يمكن تزويره بدون المفتاح السري
@Component
public class JwtUtil {

    // المفتاح السري لتوقيع الـ token - يأتي من Vault أو application.yml
    @Value("${app.jwt.secret}")
    private String jwtSecret;

    // مدة صلاحية الـ token بالميلي ثانية (افتراضياً 24 ساعة)
    @Value("${app.jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    // تحويل السلسلة النصية إلى مفتاح تشفير
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    // إنشاء token جديد بعد تسجيل الدخول بنجاح
    public String generateToken(String username, String role) {
        return Jwts.builder()
                .subject(username)
                .claim("role", role)                              // نضيف الدور كبيانات إضافية
                .issuedAt(new Date())                            // وقت الإنشاء
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey())                       // التوقيع الرقمي
                .compact();
    }

    // استخراج اسم المستخدم من الـ token
    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    // التحقق من أن الـ token صالح وغير منتهي الصلاحية وغير مُزوَّر
    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // الـ token منتهي الصلاحية أو مُزوَّر أو فارغ
            return false;
        }
    }

    // فكّ تشفير الـ token واستخراج محتواه
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

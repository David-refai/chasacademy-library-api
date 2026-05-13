package com.library.api.config;

import com.library.api.security.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
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

// الإعداد الرئيسي لأمان التطبيق
// يحدد من يستطيع الوصول لأي endpoint ويُدمج الـ JWT مع Spring Security
@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // يُفعّل @PreAuthorize في الـ controllers
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // نُعطّل CSRF لأن REST APIs لا تستخدم sessions - الـ JWT يتولى الأمان
                .csrf(csrf -> csrf.disable())

                // نُطبّق سياسة CORS المُحددة في corsConfigurationSource()
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // نُحدد من يستطيع الوصول لأي endpoint
                .authorizeHttpRequests(auth -> auth
                        // /api/auth/** مفتوح للجميع (تسجيل الدخول وإنشاء حساب)
                        .requestMatchers("/api/auth/**").permitAll()
                        // H2 console مفتوح فقط في بيئة التطوير
                        .requestMatchers("/h2-console/**").permitAll()
                        // health check مفتوح للمراقبة
                        .requestMatchers("/actuator/health").permitAll()
                        // كل شيء آخر يتطلب JWT token صالح
                        .anyRequest().authenticated()
                )

                // لا نستخدم sessions - كل طلب يحمل الـ JWT معه
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // نُضيف JwtFilter قبل فلتر Spring الافتراضي للمصادقة
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)

                // نسمح بـ H2 console frames (في الإنتاج يجب تعطيل هذا)
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))

                .build();
    }

    // سياسة CORS - تُحدد أي مصادر (origins) يمكنها إرسال طلبات للـ API
    // CORS يمنع مواقع خبيثة من إرسال طلبات نيابةً عن المستخدم
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var config = new CorsConfiguration();

        // نسمح فقط لهذه العناوين بالوصول (أضف عنوان الإنتاج هنا)
        config.setAllowedOrigins(List.of(
                "http://localhost:3000",    // واجهة React
                "http://localhost:4200"     // واجهة Angular
        ));

        // نحدد الـ HTTP methods المسموح بها
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // نسمح بـ Authorization header لإرسال الـ JWT token
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));

        // يسمح للمتصفح بإرسال الـ cookies مع الطلبات عبر المصادر المختلفة
        config.setAllowCredentials(true);

        // المتصفح يُخزّن نتيجة CORS preflight لمدة ساعة (1 hour)
        config.setMaxAge(3600L);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    // BCrypt: خوارزمية تشفير كلمات المرور - القوة 12 تعني الأمان الجيد مع أداء مقبول
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    // AuthenticationManager يُستخدم في AuthService للتحقق من بيانات تسجيل الدخول
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }
}

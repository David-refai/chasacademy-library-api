package com.library.api.filter;

import io.github.bucket4j.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// فلتر Rate Limiting باستخدام خوارزمية Token Bucket من مكتبة Bucket4j
//
// كيف تعمل خوارزمية Token Bucket:
// - كل IP يمتلك "دلو" يحمل عدداً محدداً من الرموز (tokens)
// - كل طلب يستهلك رمزاً واحداً من الدلو
// - الدلو يُعاد ملؤه تدريجياً بمرور الوقت
// - إذا فرغ الدلو، يُرفض الطلب بخطأ 429 Too Many Requests
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    // كل IP يحصل على دلو منفصل مُخزَّن في الذاكرة
    // ConcurrentHashMap آمن للاستخدام مع عدة threads في نفس الوقت
    private final Map<String, Bucket> ipBuckets = new ConcurrentHashMap<>();

    // الحد الأقصى: 60 طلباً في الدقيقة لكل IP
    private static final int MAX_REQUESTS_PER_MINUTE = 60;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String clientIp = getClientIp(request);

        // نحصل على دلو هذا الـ IP أو ننشئ واحداً جديداً إذا لم يكن موجوداً
        Bucket bucket = ipBuckets.computeIfAbsent(clientIp, this::createNewBucket);

        // نحاول استهلاك رمز واحد من الدلو
        if (bucket.tryConsume(1)) {
            // يوجد رمز - نكمل معالجة الطلب
            filterChain.doFilter(request, response);
        } else {
            // الدلو فارغ - نرفض الطلب بـ 429
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\": \"Too many requests. Limit: " + MAX_REQUESTS_PER_MINUTE
                    + " requests per minute. Please wait before retrying.\"}"
            );
        }
    }

    // إنشاء دلو جديد لـ IP جديد
    // يبدأ ممتلئاً بـ 60 رمزاً ويُعاد ملؤه بـ 60 رمزاً كل دقيقة
    private Bucket createNewBucket(String ip) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(MAX_REQUESTS_PER_MINUTE)
                .refillGreedy(MAX_REQUESTS_PER_MINUTE, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    // نحصل على IP الحقيقي للعميل حتى لو كان خلف proxy أو load balancer
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // X-Forwarded-For قد يحتوي على عدة IPs - نأخذ الأول (الأصلي)
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

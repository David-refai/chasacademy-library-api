package com.library.api.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

// يجلب معلومات الكتب من Open Library API الخارجية
// يستخدم نمط Circuit Breaker لمعالجة أعطال الـ API بشكل آمن (VG)
//
// كيف يعمل Circuit Breaker (قاطع الدائرة):
// CLOSED (مغلق) = الـ API يعمل بشكل طبيعي - نُرسل الطلبات
// OPEN (مفتوح)  = الـ API فشل كثيراً - نتوقف عن الإرسال وندعو fallback مباشرة
// HALF-OPEN     = ننتظر ونُرسل طلباً تجريبياً لنرى هل عاد الـ API للعمل
@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalBookService {

    private final RestTemplate restTemplate;

    // هذا الاسم يجب أن يطابق الـ instance المُعرَّف في application.yml
    private static final String CIRCUIT_BREAKER_NAME = "openLibrary";

    // Open Library API مجاني ولا يتطلب مفتاح API
    private static final String OPEN_LIBRARY_URL = "https://openlibrary.org/search.json?isbn=%s&limit=1";

    // @CircuitBreaker: إذا فشلت الدالة عدة مرات متتالية، تُفتح الدائرة
    // وتُستدعى fallbackBookInfo مباشرةً بدون محاولة الاتصال بالـ API
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "fallbackBookInfo")
    public Map<String, Object> getBookInfoByIsbn(String isbn) {
        log.info("Fetching book info from Open Library API for ISBN: {}", isbn);

        String url = String.format(OPEN_LIBRARY_URL, isbn);

        // هذا الطلب قد يفشل إذا كان الـ API خارج الخدمة
        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);

        if (response == null) {
            return Map.of("numFound", 0, "docs", List.of());
        }

        log.info("Open Library returned {} results for ISBN: {}", response.get("numFound"), isbn);
        return response;
    }

    // Fallback: يُستدعى تلقائياً عندما تُفتح الدائرة أو عند أي خطأ
    // يجب أن تكون نفس معاملات الدالة الأصلية + Throwable في النهاية
    public Map<String, Object> fallbackBookInfo(String isbn, Throwable throwable) {
        log.warn("Open Library API is unavailable for ISBN: {}. Reason: {}", isbn, throwable.getMessage());

        // نُعيد رداً آمناً بدلاً من رمي خطأ للعميل
        return Map.of(
                "message", "External book service is temporarily unavailable. Please try again later.",
                "isbn", isbn,
                "numFound", 0
        );
    }
}

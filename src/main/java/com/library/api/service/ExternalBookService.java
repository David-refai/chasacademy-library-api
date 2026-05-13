package com.library.api.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

// Fetches book metadata from the Open Library external API
// Uses the Circuit Breaker pattern to handle external API failures gracefully (VG)
//
// Circuit Breaker states:
// CLOSED  — API is healthy, all requests pass through normally
// OPEN    — too many failures detected, fallback is called immediately without hitting the API
// HALF-OPEN — after a wait period, a few test requests are sent to check if the API recovered
@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalBookService {

    private final RestTemplate restTemplate;

    // This name must match the instance defined in application.yml under resilience4j.circuitbreaker
    private static final String CIRCUIT_BREAKER_NAME = "openLibrary";

    // Open Library API is free and requires no API key
    private static final String OPEN_LIBRARY_URL = "https://openlibrary.org/search.json?isbn=%s&limit=1";

    // @CircuitBreaker: if this method fails repeatedly, the circuit opens
    // and fallbackBookInfo is called immediately without attempting the real HTTP request
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "fallbackBookInfo")
    public Map<String, Object> getBookInfoByIsbn(String isbn) {
        log.info("Fetching book info from Open Library API for ISBN: {}", isbn);

        String url = String.format(OPEN_LIBRARY_URL, isbn);

        // This HTTP call may fail if the external service is down
        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);

        if (response == null) {
            return Map.of("numFound", 0, "docs", List.of());
        }

        log.info("Open Library returned {} result(s) for ISBN: {}", response.get("numFound"), isbn);
        return response;
    }

    // Called automatically when the circuit is open or when any exception occurs
    // Must have the same parameters as the main method plus a Throwable at the end
    public Map<String, Object> fallbackBookInfo(String isbn, Throwable throwable) {
        log.warn("Open Library API unavailable for ISBN: {}. Reason: {}", isbn, throwable.getMessage());

        // Return a safe default response — the client never sees an unhandled error
        return Map.of(
                "message", "External book service is temporarily unavailable. Please try again later.",
                "isbn", isbn,
                "numFound", 0
        );
    }
}

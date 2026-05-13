package com.library.api.controller;

import com.library.api.dto.*;
import com.library.api.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// REST controller for book management
// All endpoints require authentication (configured in SecurityConfig)
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;
    private final ExternalBookService externalBookService;

    // GET /api/books?page=0&size=10&sort=title,asc
    // Pageable reads pagination parameters automatically from the query string
    @GetMapping
    public ResponseEntity<Page<BookResponseDto>> getAllBooks(Pageable pageable) {
        return ResponseEntity.ok(bookService.getAllBooks(pageable));
    }

    // GET /api/books/{id}
    // Result is cached in Redis after the first call (@Cacheable in BookService)
    @GetMapping("/{id}")
    public ResponseEntity<BookResponseDto> getBookById(@PathVariable Long id) {
        return ResponseEntity.ok(bookService.getBookById(id));
    }

    // GET /api/books/author/{author}?page=0&size=5
    @GetMapping("/author/{author}")
    public ResponseEntity<Page<BookResponseDto>> getBooksByAuthor(
            @PathVariable String author, Pageable pageable) {
        return ResponseEntity.ok(bookService.getBooksByAuthor(author, pageable));
    }

    // POST /api/books — admin only
    // @PreAuthorize checks the role before executing the method
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BookResponseDto> createBook(@Valid @RequestBody BookRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookService.createBook(request));
    }

    // PUT /api/books/{id} — updates the book in DB and refreshes the Redis cache
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BookResponseDto> updateBook(@PathVariable Long id,
                                                       @Valid @RequestBody BookRequestDto request) {
        return ResponseEntity.ok(bookService.updateBook(id, request));
    }

    // DELETE /api/books/{id} — removes the book from DB and evicts it from the cache
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }

    // GET /api/books/{isbn}/external-info
    // Fetches enriched metadata from Open Library (VG requirement)
    // Circuit Breaker returns a safe fallback if the external API is unavailable
    @GetMapping("/{isbn}/external-info")
    public ResponseEntity<Map<String, Object>> getExternalBookInfo(@PathVariable String isbn) {
        return ResponseEntity.ok(externalBookService.getBookInfoByIsbn(isbn));
    }
}

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

// واجهة REST لإدارة الكتب
// كل الـ endpoints تتطلب مصادقة (مُعرَّف في SecurityConfig)
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;
    private final ExternalBookService externalBookService;

    // GET /api/books?page=0&size=10&sort=title,asc
    // Pageable يأخذ معاملات الـ pagination تلقائياً من query parameters
    @GetMapping
    public ResponseEntity<Page<BookResponseDto>> getAllBooks(Pageable pageable) {
        return ResponseEntity.ok(bookService.getAllBooks(pageable));
    }

    // GET /api/books/{id}
    // النتيجة تُخزَّن في Redis بعد أول استدعاء (@Cacheable في BookService)
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

    // POST /api/books - إضافة كتاب جديد (المدير فقط)
    // @PreAuthorize يتحقق من الدور قبل تنفيذ الدالة
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BookResponseDto> createBook(@Valid @RequestBody BookRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookService.createBook(request));
    }

    // PUT /api/books/{id} - تعديل كتاب (المدير فقط) + تحديث الـ cache
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BookResponseDto> updateBook(@PathVariable Long id,
                                                       @Valid @RequestBody BookRequestDto request) {
        return ResponseEntity.ok(bookService.updateBook(id, request));
    }

    // DELETE /api/books/{id} - حذف كتاب (المدير فقط) + حذف من الـ cache
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }

    // GET /api/books/{isbn}/external-info - جلب معلومات إضافية من Open Library (VG)
    // يستخدم Circuit Breaker - إذا فشل الـ API يُعيد رداً افتراضياً بدلاً من خطأ
    @GetMapping("/{isbn}/external-info")
    public ResponseEntity<Map<String, Object>> getExternalBookInfo(@PathVariable String isbn) {
        return ResponseEntity.ok(externalBookService.getBookInfoByIsbn(isbn));
    }
}

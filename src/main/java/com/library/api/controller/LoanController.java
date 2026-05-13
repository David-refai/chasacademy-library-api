package com.library.api.controller;

import com.library.api.dto.*;
import com.library.api.service.LoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// واجهة REST لاستعارة وإعادة الكتب
@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;

    // POST /api/loans - استعارة كتاب
    // المستخدم المُسجَّل دخوله يصبح تلقائياً المستعير
    @PostMapping
    public ResponseEntity<LoanResponseDto> borrowBook(@Valid @RequestBody LoanRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loanService.borrowBook(request));
    }

    // PATCH /api/loans/{id}/return - إعادة كتاب
    // نستخدم PATCH لأننا نُعدّل حقلاً واحداً فقط (returned)
    @PatchMapping("/{id}/return")
    public ResponseEntity<LoanResponseDto> returnBook(@PathVariable Long id) {
        return ResponseEntity.ok(loanService.returnBook(id));
    }

    // GET /api/loans/my?page=0&size=5 - استعاراتي الحالية (مع pagination)
    @GetMapping("/my")
    public ResponseEntity<Page<LoanResponseDto>> getMyLoans(Pageable pageable) {
        return ResponseEntity.ok(loanService.getMyLoans(pageable));
    }

    // GET /api/loans?page=0&size=20 - كل الاستعارات (المدير فقط، مع pagination)
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<LoanResponseDto>> getAllLoans(Pageable pageable) {
        return ResponseEntity.ok(loanService.getAllLoans(pageable));
    }
}

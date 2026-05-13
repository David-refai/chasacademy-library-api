package com.library.api.controller;

import com.library.api.dto.*;
import com.library.api.service.LoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// REST controller for borrowing and returning books
@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;

    // POST /api/loans — borrow a book
    // The currently authenticated user becomes the borrower automatically
    @PostMapping
    public ResponseEntity<LoanResponseDto> borrowBook(@Valid @RequestBody LoanRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loanService.borrowBook(request));
    }

    // PATCH /api/loans/{id}/return — return a book
    // PATCH is used because only the 'returned' field is being updated
    @PatchMapping("/{id}/return")
    public ResponseEntity<LoanResponseDto> returnBook(@PathVariable Long id) {
        return ResponseEntity.ok(loanService.returnBook(id));
    }

    // GET /api/loans/my?page=0&size=5 — current user's active loans (paginated)
    @GetMapping("/my")
    public ResponseEntity<Page<LoanResponseDto>> getMyLoans(Pageable pageable) {
        return ResponseEntity.ok(loanService.getMyLoans(pageable));
    }

    // GET /api/loans?page=0&size=20 — all loans in the system (admin only, paginated)
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<LoanResponseDto>> getAllLoans(Pageable pageable) {
        return ResponseEntity.ok(loanService.getAllLoans(pageable));
    }
}

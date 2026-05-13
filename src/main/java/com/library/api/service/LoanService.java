package com.library.api.service;

import com.library.api.dto.*;
import com.library.api.entity.*;
import com.library.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

// Business logic for borrowing and returning books
@Service
@RequiredArgsConstructor
public class LoanService {

    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    // Borrow a book: creates a loan record and marks the book as unavailable
    @Transactional
    public LoanResponseDto borrowBook(LoanRequestDto request) {
        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new RuntimeException("Book not found with ID: " + request.getBookId()));

        if (!book.isAvailable()) {
            throw new IllegalStateException("Book '" + book.getTitle() + "' is currently on loan");
        }

        // Get the currently logged-in user from the SecurityContext
        // The JwtFilter placed the user there after validating the token
        AppUser currentUser = getCurrentUser();

        Loan loan = Loan.builder()
                .book(book)
                .user(currentUser)
                .loanDate(LocalDate.now())
                .dueDate(request.getDueDate())
                .returned(false)
                .build();

        // Mark the book as unavailable so nobody else can borrow it
        book.setAvailable(false);
        bookRepository.save(book);

        return mapToResponse(loanRepository.save(loan));
    }

    // Return a book: marks the loan as returned and puts the book back on the shelf
    @Transactional
    public LoanResponseDto returnBook(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found with ID: " + loanId));

        if (loan.isReturned()) {
            throw new IllegalStateException("This book has already been returned");
        }

        loan.setReturned(true);
        loan.getBook().setAvailable(true);   // Put the book back on the shelf
        bookRepository.save(loan.getBook());

        return mapToResponse(loanRepository.save(loan));
    }

    // Get the current user's active loans — paginated
    public Page<LoanResponseDto> getMyLoans(Pageable pageable) {
        AppUser currentUser = getCurrentUser();
        return loanRepository.findByUserAndReturnedFalse(currentUser, pageable)
                .map(this::mapToResponse);
    }

    // Get all loans in the system — admin use only
    public Page<LoanResponseDto> getAllLoans(Pageable pageable) {
        return loanRepository.findAll(pageable).map(this::mapToResponse);
    }

    // Retrieve the AppUser entity for the currently authenticated user
    private AppUser getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found: " + username));
    }

    private LoanResponseDto mapToResponse(Loan loan) {
        var dto = new LoanResponseDto();
        dto.setId(loan.getId());
        dto.setBookId(loan.getBook().getId());
        dto.setBookTitle(loan.getBook().getTitle());
        dto.setUsername(loan.getUser().getUsername());
        dto.setLoanDate(loan.getLoanDate());
        dto.setDueDate(loan.getDueDate());
        dto.setReturned(loan.isReturned());
        return dto;
    }
}

package com.library.api.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

// Represents a book loan — links a user to a borrowed book for a specific period
@Entity
@Table(name = "loans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FetchType.LAZY means book data is not loaded from DB unless explicitly accessed
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    // The user who borrowed the book
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    // Set automatically when the loan is created
    private LocalDate loanDate;

    // Must be a future date — validated in LoanRequestDto
    private LocalDate dueDate;

    // false = book is still checked out, true = book has been returned
    @Builder.Default
    private boolean returned = false;
}

package com.library.api.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

// Input required to borrow a book
@Data
public class LoanRequestDto {

    // ID of the book to borrow — must be a positive number
    @NotNull(message = "Book ID is required")
    @Positive(message = "Book ID must be a positive number")
    private Long bookId;

    // Return date — must be a future date, not today or in the past
    @NotNull(message = "Due date is required")
    @Future(message = "Due date must be in the future")
    private LocalDate dueDate;
}

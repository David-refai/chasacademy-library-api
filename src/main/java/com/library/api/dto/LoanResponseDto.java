package com.library.api.dto;

import lombok.Data;
import java.time.LocalDate;

// Loan data returned to the client
@Data
public class LoanResponseDto {

    private Long id;
    private Long bookId;

    // Book title included for readability — avoids a second API call just to get the title
    private String bookTitle;
    private String username;

    private LocalDate loanDate;
    private LocalDate dueDate;

    // Whether the book has been returned
    private boolean returned;
}

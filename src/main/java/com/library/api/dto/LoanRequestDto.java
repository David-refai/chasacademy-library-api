package com.library.api.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

// البيانات المطلوبة لاستعارة كتاب
@Data
public class LoanRequestDto {

    // معرّف الكتاب المراد استعارته - يجب أن يكون رقماً موجباً
    @NotNull(message = "Book ID is required")
    @Positive(message = "Book ID must be a positive number")
    private Long bookId;

    // تاريخ الإعادة - يجب أن يكون في المستقبل
    @NotNull(message = "Due date is required")
    @Future(message = "Due date must be in the future")
    private LocalDate dueDate;
}

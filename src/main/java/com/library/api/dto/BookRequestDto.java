package com.library.api.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

// البيانات المطلوبة لإنشاء كتاب جديد أو تعديل كتاب موجود
// كل حقل له قواعد تحقق تُرفض تلقائياً إذا لم تُحقَّق
@Data
public class BookRequestDto {

    // العنوان مطلوب ولا يتجاوز 255 حرفاً
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title cannot exceed 255 characters")
    private String title;

    // اسم المؤلف مطلوب
    @NotBlank(message = "Author is required")
    @Size(max = 255, message = "Author name cannot exceed 255 characters")
    private String author;

    // ISBN يجب أن يكون 13 رقماً بالضبط (معيار ISBN-13)
    @NotBlank(message = "ISBN is required")
    @Pattern(regexp = "^\\d{13}$", message = "ISBN must be exactly 13 digits")
    private String isbn;

    // سنة النشر بين 1000 و 2100 (لتجنب القيم المنطقياً مستحيلة)
    @Min(value = 1000, message = "Published year must be at least 1000")
    @Max(value = 2100, message = "Published year seems too far in the future")
    private Integer publishedYear;
}

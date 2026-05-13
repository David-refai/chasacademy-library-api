package com.library.api.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

// Input required to create a new book or update an existing one
// Each field has validation rules that Spring enforces automatically via @Valid
@Data
public class BookRequestDto {

    // Title is required and cannot exceed 255 characters
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title cannot exceed 255 characters")
    private String title;

    // Author name is required
    @NotBlank(message = "Author is required")
    @Size(max = 255, message = "Author name cannot exceed 255 characters")
    private String author;

    // ISBN must be exactly 13 digits (ISBN-13 international standard)
    @NotBlank(message = "ISBN is required")
    @Pattern(regexp = "^\\d{13}$", message = "ISBN must be exactly 13 digits")
    private String isbn;

    // Published year must be between 1000 and 2100 to reject obviously invalid values
    @Min(value = 1000, message = "Published year must be at least 1000")
    @Max(value = 2100, message = "Published year seems too far in the future")
    private Integer publishedYear;
}

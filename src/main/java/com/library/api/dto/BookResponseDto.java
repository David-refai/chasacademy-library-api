package com.library.api.dto;

import lombok.Data;
import java.io.Serializable;

// Book data returned to the client
// Must implement Serializable so it can be stored in Redis as a Java object
@Data
public class BookResponseDto implements Serializable {

    private Long id;
    private String title;
    private String author;
    private String isbn;
    private Integer publishedYear;

    // Tells the client whether the book is available for borrowing
    private boolean available;
}

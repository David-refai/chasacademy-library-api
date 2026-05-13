package com.library.api.entity;

import jakarta.persistence.*;
import lombok.*;

// Represents a book in the library stored in the BOOKS table
@Entity
@Table(name = "books")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;

    // ISBN is the unique international book identifier (ISBN-13 standard)
    @Column(unique = true, nullable = false)
    private String isbn;

    private Integer publishedYear;

    // true = book is on the shelf and can be borrowed
    // false = book is currently checked out
    @Column(nullable = false)
    @Builder.Default
    private boolean available = true;
}

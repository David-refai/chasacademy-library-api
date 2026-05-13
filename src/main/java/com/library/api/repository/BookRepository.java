package com.library.api.repository;

import com.library.api.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

// JpaRepository<Book, Long> provides full CRUD + pagination support for free
public interface BookRepository extends JpaRepository<Book, Long> {

    // Returns books paginated to avoid loading thousands of records at once
    Page<Book> findByAuthor(String author, Pageable pageable);

    // Check for duplicate ISBN before saving a new book
    boolean existsByIsbn(String isbn);

    Optional<Book> findByIsbn(String isbn);
}

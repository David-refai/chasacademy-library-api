package com.library.api.service;

import com.library.api.dto.*;
import com.library.api.entity.Book;
import com.library.api.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Business logic for book management
// Uses Spring Cache annotations to store frequently accessed books in Redis
@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;

    // @Cacheable: first call hits the database and stores the result in Redis.
    // All subsequent calls for the same ID return the cached result without any DB query.
    // Cache name "books" is configured with a 30-minute TTL in RedisConfig.
    @Cacheable(value = "books", key = "#id")
    public BookResponseDto getBookById(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Book not found with ID: " + id));
        return mapToResponse(book);
    }

    // Pageable lets the client request a specific page instead of all records at once
    // Example: GET /api/books?page=0&size=10&sort=title,asc
    public Page<BookResponseDto> getAllBooks(Pageable pageable) {
        return bookRepository.findAll(pageable).map(this::mapToResponse);
    }

    // Search by author with pagination
    public Page<BookResponseDto> getBooksByAuthor(String author, Pageable pageable) {
        return bookRepository.findByAuthor(author, pageable).map(this::mapToResponse);
    }

    @Transactional
    public BookResponseDto createBook(BookRequestDto request) {
        if (bookRepository.existsByIsbn(request.getIsbn())) {
            throw new IllegalArgumentException("A book with ISBN " + request.getIsbn() + " already exists");
        }

        Book book = Book.builder()
                .title(request.getTitle())
                .author(request.getAuthor())
                .isbn(request.getIsbn())
                .publishedYear(request.getPublishedYear())
                .available(true)
                .build();

        return mapToResponse(bookRepository.save(book));
    }

    // @CachePut: updates the database AND refreshes the cache entry with the new data
    // Without this, the cache would serve stale data until the TTL expires
    @CachePut(value = "books", key = "#id")
    @Transactional
    public BookResponseDto updateBook(Long id, BookRequestDto request) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Book not found with ID: " + id));

        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setPublishedYear(request.getPublishedYear());

        return mapToResponse(bookRepository.save(book));
    }

    // @CacheEvict: removes the book's cache entry when it is deleted from the database
    // Without this, the cache would keep returning a deleted book until the TTL expires
    @CacheEvict(value = "books", key = "#id")
    @Transactional
    public void deleteBook(Long id) {
        if (!bookRepository.existsById(id)) {
            throw new RuntimeException("Book not found with ID: " + id);
        }
        bookRepository.deleteById(id);
    }

    // Map a Book entity to a DTO to avoid exposing internal entity structure to clients
    private BookResponseDto mapToResponse(Book book) {
        var dto = new BookResponseDto();
        dto.setId(book.getId());
        dto.setTitle(book.getTitle());
        dto.setAuthor(book.getAuthor());
        dto.setIsbn(book.getIsbn());
        dto.setPublishedYear(book.getPublishedYear());
        dto.setAvailable(book.isAvailable());
        return dto;
    }
}

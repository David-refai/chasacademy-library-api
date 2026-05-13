package com.library.api;

import com.library.api.dto.BookRequestDto;
import com.library.api.dto.BookResponseDto;
import com.library.api.entity.Book;
import com.library.api.repository.BookRepository;
import com.library.api.service.BookService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Cache behavior tests for BookService
// Spring AOP must be active for @Cacheable / @CachePut / @CacheEvict to fire, so we
// need a minimal Spring context here — unlike BookServiceTest which uses pure Mockito.
// ConcurrentMapCacheManager replaces Redis so no external server is needed.
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {BookServiceCacheTest.CacheTestConfig.class})
class BookServiceCacheTest {

    // Minimal Spring configuration that activates the caching AOP proxy
    @Configuration
    @EnableCaching
    static class CacheTestConfig {

        @Bean
        CacheManager cacheManager() {
            // In-memory cache with the same name ("books") used in BookService
            return new ConcurrentMapCacheManager("books");
        }

        // Register BookService as a Spring bean so AOP can wrap it with a cache proxy
        @Bean
        BookService bookService(BookRepository repo) {
            return new BookService(repo);
        }
    }

    @MockBean
    private BookRepository bookRepository;

    @Autowired
    private BookService bookService;

    @Autowired
    private CacheManager cacheManager;

    private Book testBook;

    @BeforeEach
    void setUp() {
        testBook = Book.builder()
                .id(1L)
                .title("Clean Code")
                .author("Robert C. Martin")
                .isbn("9780132350884")
                .publishedYear(2008)
                .available(true)
                .build();
    }

    @AfterEach
    void clearCache() {
        // Flush cache between tests to prevent state from leaking across test cases
        Objects.requireNonNull(cacheManager.getCache("books")).clear();
    }

    // ===== @Cacheable TESTS =====

    @Test
    @DisplayName("@Cacheable: second call for the same id should not hit the database")
    void getBookById_shouldHitDatabaseOnlyOnce_whenCalledTwice() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));

        bookService.getBookById(1L); // cache miss — queries DB and stores result
        bookService.getBookById(1L); // cache hit — no DB call

        // Despite two method calls, the repository must be queried only once
        verify(bookRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("@Cacheable: cached result must equal the original database result")
    void getBookById_shouldReturnSameData_fromCacheAndDatabase() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));

        BookResponseDto fromDb    = bookService.getBookById(1L); // DB call
        BookResponseDto fromCache = bookService.getBookById(1L); // cache hit

        assertThat(fromCache.getId()).isEqualTo(fromDb.getId());
        assertThat(fromCache.getTitle()).isEqualTo(fromDb.getTitle());
        assertThat(fromCache.getAuthor()).isEqualTo(fromDb.getAuthor());
    }

    @Test
    @DisplayName("@Cacheable: different book ids must be cached independently")
    void getBookById_shouldCacheDifferentIdsIndependently() {
        Book book2 = Book.builder()
                .id(2L)
                .title("Refactoring")
                .author("Martin Fowler")
                .isbn("9780201485677")
                .publishedYear(2018)
                .available(true)
                .build();

        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(bookRepository.findById(2L)).thenReturn(Optional.of(book2));

        bookService.getBookById(1L);
        bookService.getBookById(2L);
        bookService.getBookById(1L); // cache hit for id=1
        bookService.getBookById(2L); // cache hit for id=2

        // Each id should trigger exactly one DB call regardless of how many times it is read
        verify(bookRepository, times(1)).findById(1L);
        verify(bookRepository, times(1)).findById(2L);
    }

    // ===== @CachePut TESTS =====

    @Test
    @DisplayName("@CachePut: getBookById after update must return the new title without an extra DB call")
    void getBookById_shouldReturnUpdatedTitle_afterCachePut() {
        // Step 1: populate the cache with the original data
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        bookService.getBookById(1L); // DB hit #1

        // Step 2: update — @CachePut must overwrite the stale cache entry
        Book updatedBook = Book.builder()
                .id(1L)
                .title("Clean Code (2nd Edition)")
                .author("Robert C. Martin")
                .isbn("9780132350884")
                .publishedYear(2008)
                .available(true)
                .build();

        BookRequestDto request = new BookRequestDto();
        request.setTitle("Clean Code (2nd Edition)");
        request.setAuthor("Robert C. Martin");
        request.setIsbn("9780132350884");
        request.setPublishedYear(2008);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(updatedBook));
        when(bookRepository.save(any(Book.class))).thenReturn(updatedBook);
        bookService.updateBook(1L, request); // DB hit #2 (findById inside update)

        // Step 3: read again — the updated value must be served from cache, no extra DB hit
        BookResponseDto result = bookService.getBookById(1L);

        assertThat(result.getTitle()).isEqualTo("Clean Code (2nd Edition)");
        // Exactly 2 findById calls: one from getBookById, one from updateBook
        // The final getBookById must be a cache hit (0 extra DB calls)
        verify(bookRepository, times(2)).findById(1L);
    }

    @Test
    @DisplayName("@CachePut: must not serve stale data after the book is updated")
    void getBookById_shouldNotReturnStaleData_afterCachePut() {
        // Cache the original book
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        bookService.getBookById(1L);

        // Update with completely different author and year
        Book updatedBook = Book.builder()
                .id(1L)
                .title("Refactoring")
                .author("Martin Fowler")
                .isbn("9780132350884")
                .publishedYear(2018)
                .available(true)
                .build();

        BookRequestDto request = new BookRequestDto();
        request.setTitle("Refactoring");
        request.setAuthor("Martin Fowler");
        request.setIsbn("9780132350884");
        request.setPublishedYear(2018);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(updatedBook));
        when(bookRepository.save(any(Book.class))).thenReturn(updatedBook);
        bookService.updateBook(1L, request);

        // Cache must now hold the new values, not the original ones
        BookResponseDto result = bookService.getBookById(1L);

        assertThat(result.getTitle()).isEqualTo("Refactoring");
        assertThat(result.getAuthor()).isEqualTo("Martin Fowler");
        assertThat(result.getPublishedYear()).isEqualTo(2018);
    }

    // ===== @CacheEvict TESTS =====

    @Test
    @DisplayName("@CacheEvict: getBookById after deleteBook must hit the database again")
    void getBookById_shouldHitDatabase_afterCacheEvict() {
        // Step 1: populate the cache
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        bookService.getBookById(1L); // DB hit #1, result cached

        // Step 2: delete — @CacheEvict must remove the cache entry for id=1
        when(bookRepository.existsById(1L)).thenReturn(true);
        bookService.deleteBook(1L);

        // Step 3: read again — entry is gone, so must go back to the database
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        bookService.getBookById(1L); // DB hit #2

        // Two findById calls expected: one before delete, one after eviction
        verify(bookRepository, times(2)).findById(1L);
    }

    @Test
    @DisplayName("@CacheEvict: deleting one book must not evict entries for other books")
    void deleteBook_shouldOnlyEvictItsOwnCacheEntry_notOthers() {
        Book book2 = Book.builder()
                .id(2L)
                .title("Refactoring")
                .author("Martin Fowler")
                .isbn("9780201485677")
                .publishedYear(2018)
                .available(true)
                .build();

        // Cache both books
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(bookRepository.findById(2L)).thenReturn(Optional.of(book2));
        bookService.getBookById(1L); // cached
        bookService.getBookById(2L); // cached

        // Delete book 1 only
        when(bookRepository.existsById(1L)).thenReturn(true);
        bookService.deleteBook(1L);

        // Reading book 2 must still be a cache hit (not evicted)
        bookService.getBookById(2L);
        verify(bookRepository, times(1)).findById(2L); // only the initial load, not re-queried
    }
}

package com.library.api;

import com.library.api.dto.*;
import com.library.api.entity.Book;
import com.library.api.repository.BookRepository;
import com.library.api.service.BookService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

// Unit tests for BookService
// Mockito is used to simulate the repository without a real database
// Tests run fast and are completely isolated from external dependencies
@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    // @Mock creates a fake BookRepository that we control
    @Mock
    private BookRepository bookRepository;

    // @InjectMocks creates BookService and injects the mock into it
    @InjectMocks
    private BookService bookService;

    // Shared test data reused across multiple tests
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

    @Test
    @DisplayName("getBookById: should return book when it exists")
    void getBookById_shouldReturnBook_whenExists() {
        // Arrange: tell the mock to return testBook when searching by id=1
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));

        // Act: call the method under test
        BookResponseDto result = bookService.getBookById(1L);

        // Assert: verify the result matches the expected data
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Clean Code");
        assertThat(result.getAuthor()).isEqualTo("Robert C. Martin");
        assertThat(result.isAvailable()).isTrue();

        // Confirm the repository was called exactly once
        verify(bookRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("getBookById: should throw RuntimeException when book does not exist")
    void getBookById_shouldThrowException_whenNotFound() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        // Expect RuntimeException with a message containing "Book not found"
        assertThatThrownBy(() -> bookService.getBookById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Book not found");
    }

    @Test
    @DisplayName("createBook: should throw IllegalArgumentException when ISBN already exists")
    void createBook_shouldThrowException_whenIsbnAlreadyExists() {
        BookRequestDto request = new BookRequestDto();
        request.setTitle("Clean Code 2nd Edition");
        request.setAuthor("Robert C. Martin");
        request.setIsbn("9780132350884");  // Same ISBN as an existing book
        request.setPublishedYear(2024);

        when(bookRepository.existsByIsbn("9780132350884")).thenReturn(true);

        assertThatThrownBy(() -> bookService.createBook(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");

        // Confirm save was never called because we rejected early
        verify(bookRepository, never()).save(any());
    }

    @Test
    @DisplayName("createBook: should save and return book when input is valid")
    void createBook_shouldSaveAndReturnBook_whenValid() {
        BookRequestDto request = new BookRequestDto();
        request.setTitle("New Book");
        request.setAuthor("New Author");
        request.setIsbn("9781234567890");
        request.setPublishedYear(2024);

        when(bookRepository.existsByIsbn("9781234567890")).thenReturn(false);

        // Simulate the repository assigning an ID after save
        Book savedBook = Book.builder()
                .id(10L)
                .title("New Book")
                .author("New Author")
                .isbn("9781234567890")
                .publishedYear(2024)
                .available(true)
                .build();
        when(bookRepository.save(any(Book.class))).thenReturn(savedBook);

        BookResponseDto result = bookService.createBook(request);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getTitle()).isEqualTo("New Book");
        assertThat(result.isAvailable()).isTrue();
    }

    @Test
    @DisplayName("getAllBooks: should return paginated results")
    void getAllBooks_shouldReturnPagedBooks() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Book> bookPage = new PageImpl<>(List.of(testBook), pageable, 1);

        when(bookRepository.findAll(pageable)).thenReturn(bookPage);

        Page<BookResponseDto> result = bookService.getAllBooks(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Clean Code");
    }

    @Test
    @DisplayName("deleteBook: should throw RuntimeException when book does not exist")
    void deleteBook_shouldThrowException_whenBookNotFound() {
        when(bookRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> bookService.deleteBook(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Book not found");

        // Confirm deleteById was never called
        verify(bookRepository, never()).deleteById(any());
    }
}

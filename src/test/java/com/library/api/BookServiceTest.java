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

// اختبارات وحدة لـ BookService
// نستخدم Mockito لمحاكاة الـ repository بدلاً من قاعدة بيانات حقيقية
// هذا يجعل الاختبارات سريعة ومعزولة عن التبعيات الخارجية
@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    // @Mock ينشئ نسخة وهمية من BookRepository تتحكم في إجاباتها
    @Mock
    private BookRepository bookRepository;

    // @InjectMocks ينشئ BookService ويُدخل الـ mock فيه
    @InjectMocks
    private BookService bookService;

    // بيانات الاختبار - نُعيد استخدامها في عدة اختبارات
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
    @DisplayName("getBookById: يجب أن يُعيد الكتاب عندما يكون موجوداً")
    void getBookById_shouldReturnBook_whenExists() {
        // Arrange: نُعلّم الـ mock أن يُعيد testBook عند البحث بـ id=1
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));

        // Act: نستدعي الدالة المراد اختبارها
        BookResponseDto result = bookService.getBookById(1L);

        // Assert: نتحقق أن النتيجة صحيحة
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Clean Code");
        assertThat(result.getAuthor()).isEqualTo("Robert C. Martin");
        assertThat(result.isAvailable()).isTrue();

        // نتأكد أن الـ repository استُدعي مرة واحدة فقط
        verify(bookRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("getBookById: يجب أن يرمي خطأ عندما لا يوجد الكتاب")
    void getBookById_shouldThrowException_whenNotFound() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        // نتوقع أن تُرمى RuntimeException مع رسالة تحتوي على "Book not found"
        assertThatThrownBy(() -> bookService.getBookById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Book not found");
    }

    @Test
    @DisplayName("createBook: يجب أن يرفض الكتاب إذا كان الـ ISBN موجوداً مسبقاً")
    void createBook_shouldThrowException_whenIsbnAlreadyExists() {
        // نُحضّر طلب إنشاء كتاب بـ ISBN موجود
        BookRequestDto request = new BookRequestDto();
        request.setTitle("Clean Code 2nd Edition");
        request.setAuthor("Robert C. Martin");
        request.setIsbn("9780132350884");  // نفس الـ ISBN
        request.setPublishedYear(2024);

        // الـ repository يقول أن هذا الـ ISBN موجود مسبقاً
        when(bookRepository.existsByIsbn("9780132350884")).thenReturn(true);

        // يجب أن يرمي IllegalArgumentException
        assertThatThrownBy(() -> bookService.createBook(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");

        // نتأكد أن save لم يُستدعَ لأننا رفضنا قبل الوصول إليه
        verify(bookRepository, never()).save(any());
    }

    @Test
    @DisplayName("createBook: يجب أن يحفظ الكتاب ويُعيده عند بيانات صحيحة")
    void createBook_shouldSaveAndReturnBook_whenValid() {
        BookRequestDto request = new BookRequestDto();
        request.setTitle("New Book");
        request.setAuthor("New Author");
        request.setIsbn("9781234567890");
        request.setPublishedYear(2024);

        // الـ ISBN غير موجود
        when(bookRepository.existsByIsbn("9781234567890")).thenReturn(false);

        // الـ repository يُعيد الكتاب بعد الحفظ مع id مُعيَّن
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
    @DisplayName("getAllBooks: يجب أن يُعيد الكتب بشكل مُقسَّم (paginated)")
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
    @DisplayName("deleteBook: يجب أن يرمي خطأ عند محاولة حذف كتاب غير موجود")
    void deleteBook_shouldThrowException_whenBookNotFound() {
        when(bookRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> bookService.deleteBook(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Book not found");

        verify(bookRepository, never()).deleteById(any());
    }
}

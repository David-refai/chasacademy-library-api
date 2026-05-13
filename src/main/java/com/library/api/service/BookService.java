package com.library.api.service;

import com.library.api.dto.*;
import com.library.api.entity.Book;
import com.library.api.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// منطق الأعمال لإدارة الكتب
// يستخدم Redis Cache لتسريع الاستعلامات المتكررة
@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;

    // @Cacheable: في أول مرة يُجلب الكتاب من DB ويُخزَّن في Redis
    // في المرات التالية يُرجَع مباشرةً من Redis بدون استعلام DB
    // المفتاح = id الكتاب، اسم الـ cache = "books" (مُعرَّف في RedisConfig)
    @Cacheable(value = "books", key = "#id")
    public BookResponseDto getBookById(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Book not found with ID: " + id));
        return mapToResponse(book);
    }

    // Pageable يسمح للعميل بطلب صفحة معينة بدلاً من كل السجلات دفعة واحدة
    // مثال: GET /api/books?page=0&size=10&sort=title,asc
    public Page<BookResponseDto> getAllBooks(Pageable pageable) {
        return bookRepository.findAll(pageable).map(this::mapToResponse);
    }

    // بحث حسب المؤلف مع pagination
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

    // @CachePut: يُحدّث البيانات في DB ويُحدّث الـ cache في نفس الوقت
    // بدونه سيبقى الـ cache قديماً حتى ينتهي الـ TTL
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

    // @CacheEvict: يحذف سجل الكتاب من الـ cache عند حذفه من DB
    // بدونه سيستمر الـ cache بإرجاع بيانات كتاب محذوف
    @CacheEvict(value = "books", key = "#id")
    @Transactional
    public void deleteBook(Long id) {
        if (!bookRepository.existsById(id)) {
            throw new RuntimeException("Book not found with ID: " + id);
        }
        bookRepository.deleteById(id);
    }

    // تحويل الكيان (Entity) إلى DTO لتجنب تسريب التفاصيل الداخلية للعميل
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

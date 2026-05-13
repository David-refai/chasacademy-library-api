package com.library.api.repository;

import com.library.api.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

// JpaRepository<Book, Long> يعني: كيان من نوع Book، مفتاحه الأساسي Long
// يأتي مع CRUD كامل + دعم Pagination جاهز مجاناً
public interface BookRepository extends JpaRepository<Book, Long> {

    // يرجع الكتب بشكل مُقسَّم (paginated) لتفادي إرسال آلاف السجلات دفعة واحدة
    Page<Book> findByAuthor(String author, Pageable pageable);

    // للتحقق من عدم تكرار ISBN قبل حفظ كتاب جديد
    boolean existsByIsbn(String isbn);

    Optional<Book> findByIsbn(String isbn);
}

package com.library.api.config;

import com.library.api.entity.*;
import com.library.api.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// يعمل عند بدء التطبيق لإنشاء بيانات تجريبية في قاعدة البيانات
// مفيد لاختبار التطبيق بدون إدخال بيانات يدوي
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        seedUsers();
        seedBooks();
    }

    private void seedUsers() {
        // ننشئ المستخدمين فقط إذا لم يكونوا موجودين
        if (userRepository.count() > 0) return;

        // حساب المدير - يمكنه إضافة وتعديل وحذف الكتب
        userRepository.save(AppUser.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin1234"))
                .role("ROLE_ADMIN")
                .build());

        // حساب مستخدم عادي - يمكنه استعارة الكتب فقط
        userRepository.save(AppUser.builder()
                .username("user1")
                .password(passwordEncoder.encode("user1234"))
                .role("ROLE_USER")
                .build());

        log.info("Created default users: admin/admin1234 and user1/user1234");
    }

    private void seedBooks() {
        if (bookRepository.count() > 0) return;

        bookRepository.save(Book.builder()
                .title("Clean Code")
                .author("Robert C. Martin")
                .isbn("9780132350884")
                .publishedYear(2008)
                .available(true)
                .build());

        bookRepository.save(Book.builder()
                .title("The Pragmatic Programmer")
                .author("David Thomas")
                .isbn("9780201616224")
                .publishedYear(1999)
                .available(true)
                .build());

        bookRepository.save(Book.builder()
                .title("Design Patterns")
                .author("Gang of Four")
                .isbn("9780201633610")
                .publishedYear(1994)
                .available(true)
                .build());

        bookRepository.save(Book.builder()
                .title("Spring in Action")
                .author("Craig Walls")
                .isbn("9781617294945")
                .publishedYear(2022)
                .available(true)
                .build());

        bookRepository.save(Book.builder()
                .title("Effective Java")
                .author("Joshua Bloch")
                .isbn("9780134685991")
                .publishedYear(2018)
                .available(true)
                .build());

        log.info("Created 5 sample books");
    }
}

package com.library.api.repository;

import com.library.api.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

// Spring Data JPA يُنشئ تلقائياً كود SQL بناءً على أسماء الدوال
// لا نحتاج لكتابة SQL يدوياً - Spring يفهم الاسم ويبني الاستعلام
public interface UserRepository extends JpaRepository<AppUser, Long> {

    // SELECT * FROM users WHERE username = ?
    Optional<AppUser> findByUsername(String username);

    // SELECT COUNT(*) > 0 FROM users WHERE username = ?
    // يُستخدم للتحقق من عدم تكرار اسم المستخدم عند التسجيل
    boolean existsByUsername(String username);
}

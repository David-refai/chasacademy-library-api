package com.library.api.repository;

import com.library.api.entity.AppUser;
import com.library.api.entity.Loan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    // استعارات مستخدم معيّن التي لم تُعَد بعد - مع pagination
    Page<Loan> findByUserAndReturnedFalse(AppUser user, Pageable pageable);

    // هل هذا الكتاب مُستعار حالياً من شخص آخر؟
    boolean existsByBookIdAndReturnedFalse(Long bookId);
}

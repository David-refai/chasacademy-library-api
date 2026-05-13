package com.library.api.repository;

import com.library.api.entity.AppUser;
import com.library.api.entity.Loan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    // Active (not-yet-returned) loans for a specific user — paginated
    Page<Loan> findByUserAndReturnedFalse(AppUser user, Pageable pageable);

    // Check if a book is currently checked out by anyone
    boolean existsByBookIdAndReturnedFalse(Long bookId);
}

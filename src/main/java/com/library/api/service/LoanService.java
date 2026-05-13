package com.library.api.service;

import com.library.api.dto.*;
import com.library.api.entity.*;
import com.library.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

// منطق الأعمال لاستعارة وإعادة الكتب
@Service
@RequiredArgsConstructor
public class LoanService {

    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    // استعارة كتاب: يُنشئ سجل استعارة ويُعلّم الكتاب كـ "غير متاح"
    @Transactional
    public LoanResponseDto borrowBook(LoanRequestDto request) {
        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new RuntimeException("Book not found with ID: " + request.getBookId()));

        if (!book.isAvailable()) {
            throw new IllegalStateException("Book '" + book.getTitle() + "' is currently on loan");
        }

        // نحصل على بيانات المستخدم المُسجَّل الدخول من الـ SecurityContext
        AppUser currentUser = getCurrentUser();

        Loan loan = Loan.builder()
                .book(book)
                .user(currentUser)
                .loanDate(LocalDate.now())
                .dueDate(request.getDueDate())
                .returned(false)
                .build();

        // نُعلّم الكتاب كغير متاح حتى لا يستطيع شخص آخر استعارته
        book.setAvailable(false);
        bookRepository.save(book);

        return mapToResponse(loanRepository.save(loan));
    }

    // إعادة كتاب: يُعلّم الاستعارة كمنتهية ويُعيد الكتاب للرف
    @Transactional
    public LoanResponseDto returnBook(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found with ID: " + loanId));

        if (loan.isReturned()) {
            throw new IllegalStateException("This book has already been returned");
        }

        loan.setReturned(true);
        loan.getBook().setAvailable(true);   // نُعيد الكتاب للرف
        bookRepository.save(loan.getBook());

        return mapToResponse(loanRepository.save(loan));
    }

    // استعارات المستخدم الحالي مع pagination
    public Page<LoanResponseDto> getMyLoans(Pageable pageable) {
        AppUser currentUser = getCurrentUser();
        return loanRepository.findByUserAndReturnedFalse(currentUser, pageable)
                .map(this::mapToResponse);
    }

    // كل الاستعارات في النظام - للمدير فقط
    public Page<LoanResponseDto> getAllLoans(Pageable pageable) {
        return loanRepository.findAll(pageable).map(this::mapToResponse);
    }

    // نحصل على المستخدم المُسجَّل دخوله من الـ SecurityContext
    // الـ JwtFilter وضع المستخدم هناك بعد التحقق من الـ token
    private AppUser getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found: " + username));
    }

    private LoanResponseDto mapToResponse(Loan loan) {
        var dto = new LoanResponseDto();
        dto.setId(loan.getId());
        dto.setBookId(loan.getBook().getId());
        dto.setBookTitle(loan.getBook().getTitle());
        dto.setUsername(loan.getUser().getUsername());
        dto.setLoanDate(loan.getLoanDate());
        dto.setDueDate(loan.getDueDate());
        dto.setReturned(loan.isReturned());
        return dto;
    }
}

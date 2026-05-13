package com.library.api;

import com.library.api.dto.LoanRequestDto;
import com.library.api.dto.LoanResponseDto;
import com.library.api.entity.*;
import com.library.api.repository.*;
import com.library.api.service.LoanService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Unit tests for LoanService — covers borrowing and returning books
@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LoanService loanService;

    private AppUser testUser;
    private Book testBook;

    @BeforeEach
    void setUp() {
        testUser = AppUser.builder()
                .id(1L)
                .username("user1")
                .password("$2a$hashed")
                .role("ROLE_USER")
                .build();

        testBook = Book.builder()
                .id(1L)
                .title("Clean Code")
                .author("Robert C. Martin")
                .isbn("9780132350884")
                .publishedYear(2008)
                .available(true)
                .build();

        // Simulate an authenticated user in the SecurityContext
        // This is what JwtFilter does on every real request
        Authentication auth = new UsernamePasswordAuthenticationToken("user1", null, List.of());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void tearDown() {
        // Clean up SecurityContext after each test to avoid leaking state
        SecurityContextHolder.clearContext();
    }

    // ===== BORROW TESTS =====

    @Test
    @DisplayName("borrowBook: should create loan and mark book as unavailable")
    void borrowBook_shouldCreateLoan_whenBookIsAvailable() {
        LoanRequestDto request = new LoanRequestDto();
        request.setBookId(1L);
        request.setDueDate(LocalDate.now().plusDays(14));

        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(testUser));
        when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> {
            Loan loan = inv.getArgument(0);
            loan.setId(10L);
            return loan;
        });

        LoanResponseDto result = loanService.borrowBook(request);

        assertThat(result.getBookTitle()).isEqualTo("Clean Code");
        assertThat(result.getUsername()).isEqualTo("user1");
        assertThat(result.isReturned()).isFalse();
        assertThat(result.getLoanDate()).isEqualTo(LocalDate.now());

        // The book must be marked as unavailable after borrowing
        assertThat(testBook.isAvailable()).isFalse();
        verify(bookRepository).save(testBook);
    }

    @Test
    @DisplayName("borrowBook: should throw IllegalStateException when book is already on loan")
    void borrowBook_shouldThrow_whenBookIsNotAvailable() {
        testBook.setAvailable(false);  // Book is already checked out

        LoanRequestDto request = new LoanRequestDto();
        request.setBookId(1L);
        request.setDueDate(LocalDate.now().plusDays(14));

        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));

        assertThatThrownBy(() -> loanService.borrowBook(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("currently on loan");

        // Confirm no loan was saved
        verify(loanRepository, never()).save(any());
    }

    @Test
    @DisplayName("borrowBook: should throw RuntimeException when book does not exist")
    void borrowBook_shouldThrow_whenBookNotFound() {
        LoanRequestDto request = new LoanRequestDto();
        request.setBookId(99L);
        request.setDueDate(LocalDate.now().plusDays(7));

        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanService.borrowBook(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Book not found");
    }

    // ===== RETURN TESTS =====

    @Test
    @DisplayName("returnBook: should mark loan as returned and make book available again")
    void returnBook_shouldMarkReturnedAndRestoreAvailability() {
        testBook.setAvailable(false);  // Book is currently checked out

        Loan activeLoan = Loan.builder()
                .id(1L)
                .book(testBook)
                .user(testUser)
                .loanDate(LocalDate.now().minusDays(7))
                .dueDate(LocalDate.now().plusDays(7))
                .returned(false)
                .build();

        when(loanRepository.findById(1L)).thenReturn(Optional.of(activeLoan));
        when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));

        LoanResponseDto result = loanService.returnBook(1L);

        assertThat(result.isReturned()).isTrue();

        // Book must be back on the shelf after return
        assertThat(testBook.isAvailable()).isTrue();
        verify(bookRepository).save(testBook);
    }

    @Test
    @DisplayName("returnBook: should throw IllegalStateException when book is already returned")
    void returnBook_shouldThrow_whenAlreadyReturned() {
        Loan alreadyReturned = Loan.builder()
                .id(1L)
                .book(testBook)
                .user(testUser)
                .loanDate(LocalDate.now().minusDays(14))
                .dueDate(LocalDate.now().minusDays(1))
                .returned(true)  // Already returned
                .build();

        when(loanRepository.findById(1L)).thenReturn(Optional.of(alreadyReturned));

        assertThatThrownBy(() -> loanService.returnBook(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already been returned");
    }

    @Test
    @DisplayName("returnBook: should throw RuntimeException when loan does not exist")
    void returnBook_shouldThrow_whenLoanNotFound() {
        when(loanRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanService.returnBook(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Loan not found");
    }
}

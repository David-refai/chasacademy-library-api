package com.library.api.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

// كيان الاستعارة - يربط مستخدماً بكتاب معيّن لفترة محددة
@Entity
@Table(name = "loans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // الكتاب المُستعار - FetchType.LAZY يعني أننا لا نجلب بيانات الكتاب من DB إلا عند الحاجة
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    // المستخدم الذي استعار الكتاب
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    // تاريخ الاستعارة - يُعيَّن تلقائياً عند إنشاء السجل
    private LocalDate loanDate;

    // تاريخ الإعادة المتوقع - يجب أن يكون في المستقبل
    private LocalDate dueDate;

    // false = الكتاب لم يُعَد بعد، true = تمّت الإعادة
    @Builder.Default
    private boolean returned = false;
}

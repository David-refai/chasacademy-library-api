package com.library.api.entity;

import jakarta.persistence.*;
import lombok.*;

// كيان الكتاب - يمثّل كتاباً في المكتبة
@Entity
@Table(name = "books")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;

    // ISBN هو رقم تعريف الكتاب الدولي - فريد لكل كتاب
    @Column(unique = true, nullable = false)
    private String isbn;

    private Integer publishedYear;

    // true = الكتاب موجود على الرف ويمكن استعارته
    // false = الكتاب مُستعار حالياً
    @Column(nullable = false)
    @Builder.Default
    private boolean available = true;
}

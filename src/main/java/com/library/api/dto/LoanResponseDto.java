package com.library.api.dto;

import lombok.Data;
import java.time.LocalDate;

// بيانات الاستعارة التي تُرسَل للعميل
@Data
public class LoanResponseDto {

    private Long id;
    private Long bookId;

    // عنوان الكتاب لسهولة القراءة - لا نُرسل المعرف فقط
    private String bookTitle;
    private String username;

    private LocalDate loanDate;
    private LocalDate dueDate;

    // هل أُعيد الكتاب أم لا
    private boolean returned;
}

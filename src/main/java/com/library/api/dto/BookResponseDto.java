package com.library.api.dto;

import lombok.Data;
import java.io.Serializable;

// بيانات الكتاب التي تُرسَل للعميل
// يجب أن يُطبّق Serializable حتى يمكن تخزينه في Redis
@Data
public class BookResponseDto implements Serializable {

    private Long id;
    private String title;
    private String author;
    private String isbn;
    private Integer publishedYear;

    // يُخبر العميل هل الكتاب متاح للاستعارة أم لا
    private boolean available;
}

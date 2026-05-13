package com.library.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

// الرد المُرسَل للعميل بعد تسجيل الدخول بنجاح
// يحتوي على JWT token الذي يجب إرساله مع كل طلب لاحق
@Data
@AllArgsConstructor
public class AuthResponseDto {

    // الـ token هو تذكرة الدخول - العميل يحفظها ويرسلها في header كل طلب
    private String token;

    private String username;

    // الدور يساعد الواجهة الأمامية في عرض أو إخفاء بعض الأزرار
    private String role;
}

package com.library.api.entity;

import jakarta.persistence.*;
import lombok.*;

// كيان المستخدم - يُخزَّن في جدول USERS في قاعدة البيانات
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // اسم المستخدم يجب أن يكون فريداً في كل قاعدة البيانات
    @Column(unique = true, nullable = false)
    private String username;

    // كلمة المرور مُشفّرة بخوارزمية BCrypt - لا نخزّن كلمات المرور كنص عادي أبداً
    @Column(nullable = false)
    private String password;

    // الدور: ROLE_USER للمستخدم العادي، ROLE_ADMIN للمدير
    @Column(nullable = false)
    private String role;
}

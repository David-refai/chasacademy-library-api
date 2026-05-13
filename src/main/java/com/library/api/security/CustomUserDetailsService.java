package com.library.api.security;

import com.library.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

// يخبر Spring Security كيفية تحميل بيانات المستخدم من قاعدة البيانات
// يُستخدم عند تسجيل الدخول وعند التحقق من الـ JWT في كل طلب
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // ابحث عن المستخدم في قاعدة البيانات
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // حوّل AppUser إلى UserDetails الذي يفهمه Spring Security
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                // مثال: ROLE_ADMIN تُصبح GrantedAuthority تُستخدم في @PreAuthorize
                List.of(new SimpleGrantedAuthority(user.getRole()))
        );
    }
}

package com.library.api.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// فلتر يعمل مرة واحدة لكل طلب HTTP ويتحقق من وجود JWT token صالح
// إذا وُجد token صالح، يُصادَق المستخدم تلقائياً لهذا الطلب
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // نقرأ Authorization header مثل: "Bearer eyJhbGciOiJIUzI1..."
        String authHeader = request.getHeader("Authorization");

        // إذا لم يكن هناك token، نكمل معالجة الطلب بدون مصادقة
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // نزيل "Bearer " لنحصل على الـ token نفسه
        String token = authHeader.substring(7);

        // نتحقق من الـ token ونُصادق المستخدم إذا كان صالحاً
        if (jwtUtil.isTokenValid(token)) {
            String username = jwtUtil.extractUsername(token);
            var userDetails = userDetailsService.loadUserByUsername(username);

            // نُنشئ كائن المصادقة ونضعه في الـ SecurityContext
            // هذا يُخبر Spring Security بأن المستخدم مُصادَق لهذا الطلب
            var authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities()
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}

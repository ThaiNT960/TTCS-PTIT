package com.example.ptitsocialchat.config;

import com.example.ptitsocialchat.entity.User;
import com.example.ptitsocialchat.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    @org.springframework.context.annotation.Lazy
    private UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // Lấy token JWT từ Cookie
            String jwt = tokenProvider.resolveToken(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                // Trích xuất username từ JWT
                String username = tokenProvider.getUsernameFromJWT(jwt);

                // Lấy thông tin user từ database
                User user = userService.findByUsername(username).orElse(null);
                if (user != null) {
                    // Tạo danh sách quyền (authorities) từ role của user
                    String userRole = user.getRole();
                    if (userRole == null || userRole.trim().isEmpty()) {
                        userRole = "ROLE_USER";
                    }
                    List<SimpleGrantedAuthority> authorities = Collections
                            .singletonList(new SimpleGrantedAuthority(userRole));

                    // Tạo đối tượng Authentication và set vào SecurityContext
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            username, null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception ex) {
            System.err.println("Could not set user authentication in security context: " + ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}

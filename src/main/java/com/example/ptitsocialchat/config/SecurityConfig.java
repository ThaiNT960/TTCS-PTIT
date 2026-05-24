package com.example.ptitsocialchat.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())  // tắt csrf khi dùng API
            .cors(org.springframework.security.config.Customizer.withDefaults()) // kích hoạt CORS cho Cookie Credentials
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // không lưu session trên server
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/api/auth/login",
                        "/api/auth/register",
                        "/ws/**",
                        "/",
                        "/index.html",
                        "/*.html",
                        "/css/**",
                        "/js/**",
                        "/img/**",
                        "/uploads/**"
                ).permitAll() // cho phép các API auth, websocket và tài nguyên giao diện tĩnh truy cập tự do
                .anyRequest().authenticated() // các request khác bắt buộc phải đăng nhập
            )
            .formLogin(login -> login.disable())
            .httpBasic(httpBasic -> httpBasic.disable());

        // Thêm bộ lọc JWT chạy trước bộ lọc UsernamePasswordAuthenticationFilter
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
package com.example.ptitsocialchat.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {

    // Khóa bí mật tĩnh (độ dài tối thiểu 256-bit để dùng với HS256)
    private static final String JWT_SECRET = "9a4f2c8d3b7a1e5f9g2h6i0j4k8l2m6n0o4p8q2r6s0t4u8v2w6x0y4z8a2b6c0d";
    private final Key key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes());
    
    // Thời hạn token là 24 giờ (86400000 ms)
    private static final long JWT_EXPIRATION = 86400000L;

    // Tên Cookie lưu trữ JWT
    public static final String COOKIE_NAME = "accessToken";

    // Sinh JWT Token từ username
    public String generateToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + JWT_EXPIRATION);

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // Lấy username từ JWT
    public String getUsernameFromJWT(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    // Xác thực tính hợp lệ của JWT Token
    public boolean validateToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(authToken);
            return true;
        } catch (MalformedJwtException ex) {
            System.err.println("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            System.err.println("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            System.err.println("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            System.err.println("JWT claims string is empty.");
        }
        return false;
    }

    // Trích xuất JWT Token từ Cookie của Request
    public String resolveToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}

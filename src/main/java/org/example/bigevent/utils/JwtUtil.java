package org.example.bigevent.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtil {

    private final SecretKey secretKey;

    public JwtUtil(@Value("${jwt.secret-key}") String key) {
        this.secretKey = Keys.hmacShaKeyFor(key.getBytes());
    }

    public String genToken(Map<String, Object> claims) {
        return Jwts.builder()
                .addClaims(claims)
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Map<String, Object> parseToken(String token) {
        try {
            Claims body = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return body;
        } catch (JwtException e) {
            throw new RuntimeException("Token 验证失败：" + e.getMessage());
        }
    }

    public String genToken(Map<String, Object> claims, long expirationMs) {
        return Jwts.builder()
                .addClaims(claims)
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }
}

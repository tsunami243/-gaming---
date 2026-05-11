package org.example.bigevent.utils;

// 👇 替换原来的 auth0 包
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

public class JwtUtil {

    // ⚠️ 关键：密钥长度必须 ≥ 32 字符（256位），原来的 "tsunami" 太短会报错！
    private static final String KEY = "tsunami1234567890abcdefghijklmnopqrstuvwxyz";
    // 生成安全的 HMAC 密钥（jjwt 要求用 SecretKey 类型）
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(KEY.getBytes());
    // 过期时间：12 小时（和原来保持一致）
    private static final long EXPIRATION_TIME = 1000 * 60 * 60 * 12;

    // 生成 Token：功能和原来完全一致，把业务数据存在 "claims" 字段里
    public static String genToken(Map<String, Object> claims) {
        return Jwts.builder()
                // 对应原来的 .withClaim("claims", claims)
                .addClaims (claims)
                // 对应原来的 .withExpiresAt(...)
                .setExpiration(new Date(System.currentTimeMillis() + 1000*60*60))
                // 对应原来的 .sign(Algorithm.HMAC256(KEY))
                .signWith(SECRET_KEY, SignatureAlgorithm.HS256)
                .compact(); // 生成最终 Token 字符串
    }

    // 解析 Token：验证后提取数据并转成 Map
    public static Map<String, Object> parseToken(String token) {
        try {
            Claims body = Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY) // 设置验证密钥
                    .build()
                    .parseClaimsJws(token) // 验证并解析 Token
                    .getBody(); // 获取载荷（Payload）
            // 直接返回body，因为生成Token时使用addClaims将数据添加到payload根级别
            return body;
        } catch (JwtException e) {
            // 捕获验证失败（过期、篡改、签名错误等）
            throw new RuntimeException("Token 验证失败：" + e.getMessage());
        }
    }

    // 生成带自定义过期时间的 Token（用于 RememberMe）
    public static String genToken(Map<String, Object> claims, long expirationMs) {
        return Jwts.builder()
                .addClaims(claims)
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(SECRET_KEY, SignatureAlgorithm.HS256)
                .compact();
    }
}
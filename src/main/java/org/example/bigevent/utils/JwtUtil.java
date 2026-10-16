package org.example.bigevent.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

/**
 * JWT 工具：负责签发与解析基于 HS256 的令牌。
 *
 * <p>签名密钥来自配置项 {@code jwt.secret-key}，长度不足 HS256 要求时会直接启动失败，
 * 属于有意的 fail-fast 设计——密钥过弱不允许上线。</p>
 */
@Component
public class JwtUtil {

    private final SecretKey secretKey;

    /**
     * 构造工具并派生 HMAC 密钥。
     *
     * @param key 配置的签名密钥明文
     */
    public JwtUtil(@Value("${jwt.secret-key}") String key) {
        this.secretKey = Keys.hmacShaKeyFor(key.getBytes());
    }

    /**
     * 签发令牌，有效期 1 小时。
     *
     * @param claims 载荷声明（如 userId、username、role）
     * @return 紧凑格式的 JWT
     */
    public String genToken(Map<String, Object> claims) {
        return Jwts.builder()
                .addClaims(claims)
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 签发令牌并指定有效期（用于「记住我」等长期会话）。
     *
     * @param claims       载荷声明
     * @param expirationMs 有效期，单位毫秒
     * @return 紧凑格式的 JWT
     */
    public String genToken(Map<String, Object> claims, long expirationMs) {
        return Jwts.builder()
                .addClaims(claims)
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析并校验令牌。
     *
     * @param token 紧凑格式的 JWT
     * @return 令牌载荷；签名不匹配、过期或格式错误时均抛出运行时异常
     * @throws RuntimeException 校验失败时抛出，消息中附带原始失败原因
     */
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
}

package org.example.bigevent.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.bigevent.pojo.User;
import org.example.bigevent.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.Map;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String KEY = "tsunami1234567890abcdefghijklmnopqrstuvwxyz";
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(KEY.getBytes());

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = request.getHeader("Authorization");

        if (token != null && !token.isEmpty()) {
            try {
                Map<String, Object> claims = JwtUtil.parseToken(token);
                Integer userId = (Integer) claims.get("userId");
                String username = (String) claims.get("username");
                String role = (String) claims.get("role");

                // 从 Redis 验证 token
                String redisToken = stringRedisTemplate.opsForValue().get("token" + userId);
                if (redisToken != null && redisToken.equals(token)) {
                    User user = new User();
                    user.setId(userId);
                    user.setUsername(username);
                    user.setRole(role);
                    user.setPassword(""); // 密码不需要设置

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (JwtException e) {
                // token 无效，继续过滤链（会返回未认证）
            }
        }

        filterChain.doFilter(request, response);
    }
}
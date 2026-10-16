package org.example.bigevent.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.bigevent.pojo.User;
import org.example.bigevent.utils.JwtUtil;
import org.example.bigevent.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

/**
 * JWT 认证过滤器：从 {@code Authorization} 头解析令牌并建立安全上下文。
 *
 * <p><b>双重校验</b>：令牌不仅签名要合法，还必须与 Redis 中的 {@code token{userId}} 一致——
 * 这样退出登录（删除 Redis 键）即可让已签发的令牌立即失效，而无需等它自然过期。</p>
 *
 * <p>任何解析失败都静默放行（不抛异常），由后续授权规则决定是否返回 401，
 * 因此公开接口携带坏令牌时仍可正常访问。</p>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 解析令牌、校验 Redis 会话并填充 SecurityContext 与 ThreadLocal。
     *
     * @param request     当前请求
     * @param response    当前响应
     * @param filterChain 过滤器链
     * @throws ServletException 下游过滤器抛出时透传
     * @throws IOException      下游读写异常时透传
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = request.getHeader("Authorization");

        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (token != null && !token.isEmpty()) {
            try {
                Map<String, Object> claims = jwtUtil.parseToken(token);
                Integer userId = (Integer) claims.get("userId");
                String username = (String) claims.get("username");
                String role = (String) claims.get("role");

                String redisToken = stringRedisTemplate.opsForValue().get("token" + userId);
                if (redisToken != null && redisToken.equals(token)) {
                    User user = new User();
                    user.setId(userId);
                    user.setUsername(username);
                    user.setRole(role);
                    user.setPassword("");

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    ThreadLocalUtil.set(claims);
                }
            } catch (Exception e) {
                // token 无效，继续过滤链（会返回未认证）
            }
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            // 线程归还池前必须清理，否则下一个请求会复用上一个用户的身份
            ThreadLocalUtil.remove();
        }
    }
}

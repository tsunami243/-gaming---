package org.example.bigevent.Interceptors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.bigevent.utils.JwtUtil;
import org.example.bigevent.utils.ThreadLocalUtil;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

@Component
public class LoginInterceptor implements HandlerInterceptor {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String loginToken = request.getHeader("Authorization");
        if (loginToken == null || loginToken.isEmpty()) {
            response.setStatus(401);
            return false;
        }
        try {
            Map<String, Object> token = JwtUtil.parseToken(loginToken);
            ThreadLocalUtil.set(token);
            //从redis中获取token
            String RedisToken = stringRedisTemplate.opsForValue().get("token"+ token.get("userId"));
            if (RedisToken == null){
                throw new RuntimeException("token不存在");
            }
            return true;
        } catch (Exception e) {
            response.setStatus(401);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {
        ThreadLocalUtil.remove();
        //防止内存泄漏
    }
}
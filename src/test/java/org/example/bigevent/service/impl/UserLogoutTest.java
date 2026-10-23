package org.example.bigevent.service.impl;

import org.example.bigevent.mapper.UserMapper;
import org.example.bigevent.utils.ThreadLocalUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * 退出登录必须让服务端会话同步失效。
 *
 * <p>验收口径：前端退出后，携带旧 JWT 的请求应因 Redis 中的 {@code token{userId}} 已被删除
 * 而在 {@code JwtAuthenticationFilter} 的双验证处失败（该过滤器行为另见
 * {@code JwtAuthenticationFilterTest}）。这里锁定失效动作本身。</p>
 */
class UserLogoutTest {

    @AfterEach
    void cleanup() {
        ThreadLocalUtil.remove();
    }

    private UserServiceImpl serviceWith(StringRedisTemplate redis) {
        UserServiceImpl service = new UserServiceImpl();
        ReflectionTestUtils.setField(service, "userMapper", mock(UserMapper.class));
        ReflectionTestUtils.setField(service, "stringRedisTemplate", redis);
        return service;
    }

    @Test
    void logoutDeletesTheCallersRedisSessionSoTheIssuedTokenStopsMatching() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ThreadLocalUtil.set(Map.of("userId", 3, "username", "moderator", "role", "ROLE_CONTENT_ADMIN"));

        serviceWith(redis).logout();

        verify(redis).delete("token3");
    }

    @Test
    void logoutOnlyTouchesTheCallersOwnKey() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ThreadLocalUtil.set(Map.of("userId", 9, "username", "other", "role", "ROLE_CONTENT_ADMIN"));

        serviceWith(redis).logout();

        verify(redis).delete("token9");
        verify(redis, never()).delete("token3");
    }

    @Test
    void absentClaimsAreRejectedInsteadOfDeletingAnything() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> serviceWith(redis).logout());

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        verifyNoInteractions(redis);
    }

    @Test
    void malformedUserIdClaimIsRejectedInsteadOfBuildingARedisKey() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ThreadLocalUtil.set(Map.of("userId", "3"));

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> serviceWith(redis).logout());

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        verifyNoInteractions(redis);
    }
}

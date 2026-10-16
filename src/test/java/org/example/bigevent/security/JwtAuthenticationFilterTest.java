package org.example.bigevent.security;

import jakarta.servlet.FilterChain;
import org.example.bigevent.pojo.User;
import org.example.bigevent.utils.JwtUtil;
import org.example.bigevent.utils.ThreadLocalUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {
    private final JwtUtil jwtUtil = mock(JwtUtil.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter();

    JwtAuthenticationFilterTest() {
        ReflectionTestUtils.setField(filter, "jwtUtil", jwtUtil);
        ReflectionTestUtils.setField(filter, "stringRedisTemplate", redis);
    }

    @AfterEach void clearContext() {
        ThreadLocalUtil.remove();
        SecurityContextHolder.clearContext();
    }

    @Test void verifiedTokenMakesTheSamePositiveUserIdAvailableToAuthorizationAndAuditDuringTheChain() throws Exception {
        String token = "verified-token";
        when(jwtUtil.parseToken(token)).thenReturn(Map.of("userId", 31, "username", "moderator", "role", "ROLE_CONTENT_ADMIN"));
        when(redis.opsForValue()).thenReturn(mock(org.springframework.data.redis.core.ValueOperations.class));
        @SuppressWarnings("unchecked")
        var values = (org.springframework.data.redis.core.ValueOperations<String, String>) redis.opsForValue();
        when(values.get("token31")).thenReturn(token);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        AtomicReference<Object> claimUserId = new AtomicReference<>();
        AtomicReference<User> principal = new AtomicReference<>();
        FilterChain chain = (req, res) -> {
            Map<?, ?> claims = ThreadLocalUtil.get();
            claimUserId.set(claims.get("userId"));
            principal.set((User) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        };

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertEquals(31, claimUserId.get());
        assertNotNull(principal.get());
        assertEquals(31, principal.get().getId());
        assertEquals("ROLE_CONTENT_ADMIN", principal.get().getRole());
        assertNull(ThreadLocalUtil.get(), "claims must be cleaned up after request completion");
    }

    @Test void malformedUserIdClaimCannotAuthenticateOrReachAuditCode() throws Exception {
        String token = "malformed-token";
        when(jwtUtil.parseToken(token)).thenReturn(Map.of("userId", "31", "username", "moderator", "role", "ROLE_CONTENT_ADMIN"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        AtomicReference<Object> claimsInChain = new AtomicReference<>(new Object());
        AtomicReference<Object> principalInChain = new AtomicReference<>(new Object());

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {
            claimsInChain.set(ThreadLocalUtil.get());
            principalInChain.set(SecurityContextHolder.getContext().getAuthentication());
        });

        assertNull(claimsInChain.get());
        assertNull(principalInChain.get());
        verifyNoInteractions(redis);
    }

    @Test void revokedOrMismatchedTokenCannotAuthenticate() throws Exception {
        String token = "revoked-token";
        when(jwtUtil.parseToken(token)).thenReturn(Map.of("userId", 31, "username", "moderator", "role", "ROLE_CONTENT_ADMIN"));
        when(redis.opsForValue()).thenReturn(mock(org.springframework.data.redis.core.ValueOperations.class));
        @SuppressWarnings("unchecked")
        var values = (org.springframework.data.redis.core.ValueOperations<String, String>) redis.opsForValue();
        when(values.get("token31")).thenReturn("another-token");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        AtomicReference<Object> claimsInChain = new AtomicReference<>(new Object());

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> claimsInChain.set(ThreadLocalUtil.get()));

        assertNull(claimsInChain.get());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}

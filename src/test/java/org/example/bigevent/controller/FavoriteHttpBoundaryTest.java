package org.example.bigevent.controller;
import org.example.bigevent.mapper.FavoriteReadMapper;
import org.example.bigevent.pojo.dto.FavoriteSummary;

import jakarta.servlet.FilterChain;
import org.example.bigevent.config.SecurityConfig;
import org.example.bigevent.security.JwtAuthenticationFilter;
import org.example.bigevent.utils.ThreadLocalUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import java.util.Map;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FavoriteV1Controller.class)
@Import(SecurityConfig.class)
class FavoriteHttpBoundaryTest {
    @Autowired MockMvc mvc;
    @MockitoBean FavoriteReadMapper mapper;
    @MockitoBean JwtAuthenticationFilter filter;
    @MockitoBean PasswordEncoder encoder;
    @BeforeEach void claimsFixture() throws Exception {
        doAnswer(call -> {
            // Emulate validated JWT claims; security authentication is supplied per request.
            ThreadLocalUtil.set(Map.of("userId", 3));
            try { ((FilterChain) call.getArgument(2)).doFilter(call.getArgument(0), call.getArgument(1)); }
            finally { ThreadLocalUtil.remove(); }
            return null;
        }).when(filter).doFilter(any(), any(), any());
    }
    @Test void anonymousCannotReadFavoritesEvenWithUserIdParameter() throws Exception {
        mvc.perform(get("/api/v1/me/favorites").param("userId", "3")).andExpect(status().isUnauthorized());
        verifyNoInteractions(mapper);
    }
    @Test void suppliedOtherUserIdCannotOverrideAuthenticatedClaims() throws Exception {
        when(mapper.page(3, 20, 0L)).thenReturn(List.of(new FavoriteSummary(7,"fixture","summary",null)));
        when(mapper.count(3)).thenReturn(1L);
        mvc.perform(get("/api/v1/me/favorites").with(user("reader").roles("USER"))
                .param("userId", "99").header("X-Trace-Id", "favorites-boundary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.traceId").value("favorites-boundary"))
                .andExpect(jsonPath("$.data.items[0].id").value(7))
                .andExpect(jsonPath("$.data.items[0].content").doesNotExist());
        verify(mapper).page(3, 20, 0L);
        verify(mapper).count(3);
        verifyNoMoreInteractions(mapper);
    }
    @Test void oversizedPageReturnsV1ErrorBeforeDatabaseAccess() throws Exception {
        mvc.perform(get("/api/v1/me/favorites").with(user("reader").roles("USER"))
                .param("pageSize", "1000"))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.traceId").isNotEmpty());
        verifyNoInteractions(mapper);
    }
}

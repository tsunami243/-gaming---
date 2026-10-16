package org.example.bigevent.controller;
import org.example.bigevent.mapper.AdminArticleMapper;
import org.example.bigevent.service.impl.AdminModerationServiceImpl;

import jakarta.servlet.FilterChain;
import org.example.bigevent.config.SecurityConfig;
import org.example.bigevent.pojo.Article;
import org.example.bigevent.security.JwtAuthenticationFilter;
import org.example.bigevent.utils.ThreadLocalUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminArticleV1Controller.class)
@Import({SecurityConfig.class, AdminModerationServiceImpl.class})
class AdminModerationHttpBoundaryTest {
    @Autowired MockMvc mvc;
    @MockitoBean AdminArticleMapper mapper;
    @MockitoBean JwtAuthenticationFilter filter;
    @MockitoBean PasswordEncoder encoder;

    @BeforeEach void passThroughWithoutClaims() throws Exception {
        doAnswer(call -> {
            ((FilterChain) call.getArgument(2)).doFilter(call.getArgument(0), call.getArgument(1));
            return null;
        }).when(filter).doFilter(any(), any(), any());
    }

    @AfterEach void clear() { ThreadLocalUtil.remove(); }

    @Test void contentAdminRoleAloneCannotModerateWithoutValidatedJwtClaims() throws Exception {
        mvc.perform(post("/api/v1/admin/articles/7/approve")
                        .with(user("test-admin").roles("CONTENT_ADMIN"))
                        .header("X-Trace-Id", "missing-actor")
                        .contentType("application/json")
                        .content("{\"revision\":3}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.traceId").value("missing-actor"));
        verifyNoInteractions(mapper);
    }

    @Test void validatedJwtClaimIsPersistedAsTheAuditActor() throws Exception {
        reset(filter);
        doAnswer(call -> {
            ThreadLocalUtil.set(Map.of("userId", 31));
            try {
                ((FilterChain) call.getArgument(2)).doFilter(call.getArgument(0), call.getArgument(1));
            } finally {
                ThreadLocalUtil.remove();
            }
            return null;
        }).when(filter).doFilter(any(), any(), any());

        Article pending = article(3, "PENDING_REVIEW");
        Article published = article(4, "PUBLISHED");
        when(mapper.findById(7)).thenReturn(pending, published);
        when(mapper.transition(eq(7), eq(3), eq("已发布"), eq("PUBLISHED"), isNull(), any())).thenReturn(1);

        mvc.perform(post("/api/v1/admin/articles/7/approve")
                        .with(user("test-admin").roles("CONTENT_ADMIN"))
                        .contentType("application/json")
                        .content("{\"revision\":3,\"reasonDetail\":\"official patch notes verified\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(7))
                .andExpect(jsonPath("$.data.revision").value(4));

        verify(mapper).audit(7, 31, "APPROVE", null, "official patch notes verified", "PENDING_REVIEW", "PUBLISHED", 4);
    }

    private static Article article(int revision, String status) {
        Article article = new Article();
        article.setId(7);
        article.setRevision(revision);
        article.setWorkflowStatus(status);
        return article;
    }
}

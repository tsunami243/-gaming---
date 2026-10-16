package org.example.bigevent.controller;

import jakarta.servlet.FilterChain;
import org.example.bigevent.config.SecurityConfig;
import org.example.bigevent.security.JwtAuthenticationFilter;
import org.example.bigevent.service.ArticleService;
import org.example.bigevent.service.CommentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CommentController.class)
@Import(SecurityConfig.class)
class CommentSubmissionGateTest {
    @Autowired MockMvc mvc;
    @MockitoBean ArticleService articles;
    @MockitoBean CommentService comments;
    @MockitoBean JwtAuthenticationFilter filter;
    @MockitoBean PasswordEncoder encoder;
    @BeforeEach void passFilter() throws Exception {
        doAnswer(call -> { ((FilterChain)call.getArgument(2)).doFilter(call.getArgument(0), call.getArgument(1)); return null; })
                .when(filter).doFilter(any(), any(), any());
    }
    @Test void disabledSubmissionRejectsAuthenticatedPostsBeforeBusinessCalls() throws Exception {
        mvc.perform(post("/comment").with(user("reader").roles("USER"))
                .contentType("application/json").content("{\"articleId\":7,\"content\":\"test\"}"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.message").value("评论提交暂未开放"));
        mvc.perform(post("/comment/7/reply").with(user("reader").roles("USER"))
                .param("parentId","1").param("content","test"))
                .andExpect(status().isServiceUnavailable());
        verifyNoInteractions(articles, comments);
    }
    @Test void readingCommentsStillWorks() throws Exception {
        when(comments.listByArticleId(7)).thenReturn(java.util.List.of());
        mvc.perform(get("/comment/7")).andExpect(status().isOk());
        verify(articles).requirePublished(7);
        verify(comments).listByArticleId(7);
    }
    @Test void anonymousPostStillRequiresAuthentication() throws Exception {
        mvc.perform(post("/comment").contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(comments);
    }
}

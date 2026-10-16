package org.example.bigevent.config;

import jakarta.servlet.FilterChain;
import org.example.bigevent.controller.ArticleController;
import org.example.bigevent.pojo.Article;
import org.example.bigevent.pojo.PageBean;
import org.example.bigevent.security.JwtAuthenticationFilter;
import org.example.bigevent.service.ArticleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ArticleController.class)
@Import(SecurityConfig.class)
class SecurityBoundaryTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArticleService articleService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void passThroughMockedJwtFilter() throws Exception {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    @Test
    void anonymousUserCannotCreateArticle() throws Exception {
        mockMvc.perform(post("/article")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validArticleJson()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void regularUserCannotCreateArticle() throws Exception {
        mockMvc.perform(post("/article")
                        .with(user("player").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validArticleJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    void regularUserCannotUpdateOrDeleteArticle() throws Exception {
        mockMvc.perform(put("/article")
                        .with(user("player").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validArticleJsonWithId()))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/article")
                        .with(user("player").roles("USER"))
                        .param("id", "1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void contentAdminCanCreateArticle() throws Exception {
        doNothing().when(articleService).addArticle(any(Article.class));

        mockMvc.perform(post("/article")
                        .with(user("editor").roles("CONTENT_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validArticleJson()))
                .andExpect(status().isOk());
    }

    @Test
    void publicReadRemainsAvailable() throws Exception {
        when(articleService.list(any(), any(), any(), any())).thenReturn(new PageBean<>());

        mockMvc.perform(get("/article").param("pageNum", "1").param("pageSize", "20"))
                .andExpect(status().isOk());
    }

    @Test
    void nonPublishedDetailReturnsNotFound() throws Exception {
        mockMvc.perform(get("/article/detail").param("id", "42"))
                .andExpect(status().isNotFound());
    }

    @Test
    void legacyPushDemoIsDeniedEvenForAuthenticatedUsers() throws Exception {
        mockMvc.perform(post("/push/simulate/batch")
                        .with(user("editor").roles("CONTENT_ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void legacyCommentLikeCountMutationIsDenied() throws Exception {
        mockMvc.perform(put("/comment/1/like")
                        .with(user("player").roles("USER"))
                        .param("likeCount", "999"))
                .andExpect(status().isForbidden());
    }

    @Test
    void regularUserCannotRunLegacyIngestConsumer() throws Exception {
        mockMvc.perform(post("/admin/ingest/consume")
                        .with(user("player").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void regularAndContentAdminUsersCannotEnumerateUserDirectory() throws Exception {
        for (String role : java.util.List.of("USER", "CONTENT_ADMIN")) {
            mockMvc.perform(get("/user/list").with(user("fixture").roles(role)))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(get("/user/list")).andExpect(status().isUnauthorized());
    }

    private static String validArticleJsonWithId() {
        return """
                {"id":1,"title":"CS2 更新","content":"这是用于权限测试的文章正文，长度足以通过当前校验规则。","coverImg":"","state":"草稿","categoryId":9}
                """;
    }

    private static String validArticleJson() {
        return """
                {"title":"CS2 更新","content":"这是用于权限测试的文章正文，长度足以通过当前校验规则。","coverImg":"","state":"草稿","categoryId":9}
                """;
    }
}

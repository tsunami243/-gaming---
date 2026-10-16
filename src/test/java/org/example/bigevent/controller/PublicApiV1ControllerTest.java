package org.example.bigevent.controller;

import jakarta.servlet.FilterChain;
import org.example.bigevent.config.SecurityConfig;
import org.example.bigevent.pojo.dto.ArticleDetailResponse;
import org.example.bigevent.pojo.dto.ArticleSummaryResponse;
import org.example.bigevent.pojo.dto.GameResponse;
import org.example.bigevent.pojo.dto.PageResponse;
import org.example.bigevent.security.JwtAuthenticationFilter;
import org.example.bigevent.service.PublicArticleReadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The controller only receives requests and wraps the response envelope;
 * all read rules live in the service and are covered by PublicArticleReadServiceImplTest.
 */
@WebMvcTest(PublicApiV1Controller.class)
@Import(SecurityConfig.class)
class PublicApiV1ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PublicArticleReadService readService;
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
    void gamesAreWrappedInTheV1EnvelopeWithATraceId() throws Exception {
        when(readService.listGames()).thenReturn(List.of(
                new GameResponse("cs2", "Counter-Strike 2", "Counter-Strike 2", List.of("CS2", "反恐精英2"), true)));

        mockMvc.perform(get("/api/v1/games"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].code").value("cs2"))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(header().exists("X-Trace-Id"));
    }

    @Test
    void articleListForwardsQueryParametersToTheService() throws Exception {
        when(readService.listArticles(2, 5, "cs2", "patch_notes"))
                .thenReturn(new PageResponse<>(List.of(summary()), 1, 2, 5));

        mockMvc.perform(get("/api/v1/articles").param("pageNum", "2").param("pageSize", "5")
                        .param("gameCode", "cs2").param("contentType", "patch_notes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value(7))
                .andExpect(jsonPath("$.data.pageNum").value(2))
                .andExpect(jsonPath("$.data.pageSize").value(5));

        verify(readService).listArticles(2, 5, "cs2", "patch_notes");
    }

    @Test
    void listDefaultsAreAppliedBeforeCallingTheService() throws Exception {
        when(readService.listArticles(1, 20, null, null))
                .thenReturn(new PageResponse<>(List.of(), 0, 1, 20));

        mockMvc.perform(get("/api/v1/articles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty());

        verify(readService).listArticles(1, 20, null, null);
    }

    @Test
    void detailReturnsTheServiceContent() throws Exception {
        when(readService.articleDetail(7)).thenReturn(new ArticleDetailResponse(
                7, "CS2 更新", "摘要", List.of("cs2"), "patch_notes", "FULL_TEXT", "PUBLISHED",
                List.of("更新"), "官方来源", "en", null, null, null, null, null,
                "https://example.invalid/cs2", "正文第一段\n\n正文第二段"));

        mockMvc.perform(get("/api/v1/articles/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("正文第一段\n\n正文第二段"))
                .andExpect(jsonPath("$.data.readingMode").value("FULL_TEXT"));
    }

    @Test
    void serviceNotFoundMapsToHttp404() throws Exception {
        when(readService.articleDetail(9))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "文章不存在"));

        mockMvc.perform(get("/api/v1/articles/9")).andExpect(status().isNotFound());
    }

    @Test
    void serviceValidationFailureKeepsTheV1ErrorEnvelope() throws Exception {
        when(readService.listArticles(1, 20, null, "hardware"))
                .thenThrow(new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "unsupported contentType"));

        mockMvc.perform(get("/api/v1/articles").param("contentType", "hardware").header("X-Trace-Id", "contract-test"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.traceId").value("contract-test"));
    }

    private ArticleSummaryResponse summary() {
        return new ArticleSummaryResponse(7, "CS2 更新", "CS2 更新摘要", List.of("cs2"), "patch_notes", "FULL_TEXT",
                "PUBLISHED", List.of("CS2", "更新"), "官方来源", "en", null, null, null, null, null);
    }
}

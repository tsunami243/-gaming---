package org.example.bigevent.controller;
import org.example.bigevent.pojo.dto.ModerationAuditRow;
import org.example.bigevent.service.AdminModerationService;

import jakarta.servlet.FilterChain;
import org.example.bigevent.config.SecurityConfig;
import org.example.bigevent.pojo.Article;
import org.example.bigevent.pojo.dto.PageResponse;
import org.example.bigevent.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminArticleV1Controller.class)
@Import(SecurityConfig.class)
class AdminArticleV1ControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean AdminModerationService service;
    @MockitoBean JwtAuthenticationFilter filter;
    @MockitoBean PasswordEncoder encoder;

    @BeforeEach void pass() throws Exception { doAnswer(i -> { ((FilterChain) i.getArgument(2)).doFilter(i.getArgument(0), i.getArgument(1)); return null; }).when(filter).doFilter(any(), any(), any()); }

    @Test
    void contentAdminCanListAndApprove() throws Exception {
        Article pending = new Article(); pending.setId(7); pending.setWorkflowStatus("PENDING_REVIEW"); pending.setRevision(3);
        when(service.list(1,20,"PENDING_REVIEW")).thenReturn(new PageResponse<>(List.of(pending),1,1,20));
        when(service.approve(eq(7), any())).thenReturn(pending);
        mvc.perform(get("/api/v1/admin/articles").with(user("admin").roles("CONTENT_ADMIN")).param("workflowStatus","PENDING_REVIEW"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].workflowStatus").value("PENDING_REVIEW"));
        mvc.perform(post("/api/v1/admin/articles/7/approve").with(user("admin").roles("CONTENT_ADMIN")).contentType("application/json").content("{\"revision\":3}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(7));
    }

    @Test
    void contentAdminCanReadArticleAuditWithTraceEnvelope() throws Exception {
        ModerationAuditRow entry = new ModerationAuditRow(9L, 7, 31, "APPROVE", null, "official notes checked", "PENDING_REVIEW", "PUBLISHED", 4, java.time.LocalDateTime.of(2026, 9, 22, 10, 0));
        when(service.audit(7, 1, 20)).thenReturn(new PageResponse<>(List.of(entry), 1, 1, 20));

        mvc.perform(get("/api/v1/admin/articles/7/audit").with(user("admin").roles("CONTENT_ADMIN"))
                        .header("X-Trace-Id", "audit-read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.traceId").value("audit-read"))
                .andExpect(jsonPath("$.data.items[0].actorUserId").value(31))
                .andExpect(jsonPath("$.data.items[0].reasonDetail").value("official notes checked"));
        verify(service).audit(7, 1, 20);
    }

    @Test
    void ordinaryUserCannotAccessAdminModeration() throws Exception {
        mvc.perform(get("/api/v1/admin/articles").with(user("reader").roles("USER"))).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    @Test
    void rejectsMissingNegativeAndOverflowRevisionWithV1Envelope() throws Exception {
        for (String body : List.of("{}", "{\"revision\":-1}", "{\"revision\":2147483647}")) {
            mvc.perform(post("/api/v1/admin/articles/7/approve").with(user("admin").roles("CONTENT_ADMIN"))
                    .header("X-Trace-Id", "revision-test").contentType("application/json").content(body))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.traceId").value("revision-test"));
        }
        verifyNoInteractions(service);
    }
}

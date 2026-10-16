package org.example.bigevent.controller;
import org.example.bigevent.pojo.dto.IngestAcceptance;
import org.example.bigevent.pojo.dto.IngestReceipt;
import org.example.bigevent.pojo.dto.IngestSourcePolicyResponse;
import org.example.bigevent.service.IngestReceiptService;
import org.example.bigevent.service.IngestSourcePolicyService;

import jakarta.servlet.FilterChain;
import org.example.bigevent.config.SecurityConfig;
import org.example.bigevent.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalIngestController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "ingest.service-token=test-token")
class InternalIngestControllerTest {
    private static final String EVENT = "{\"schemaVersion\":1,\"eventId\":\"e1\",\"sourceCode\":\"official-cs2\",\"gameCodes\":[\"cs2\"],\"contentType\":\"guide\",\"originalUrl\":\"https://example.com/news/1\",\"title\":\"title\",\"summary\":\"summary\",\"decision\":\"REVIEW\",\"ruleVersion\":\"fps-v1\",\"fetchedAt\":\"2026-09-20T12:00:00Z\"}";

    @Autowired MockMvc mvc;
    @MockitoBean IngestReceiptService service;
 @MockitoBean IngestSourcePolicyService sourcePolicyService;
    @MockitoBean JwtAuthenticationFilter filter;
    @MockitoBean PasswordEncoder encoder;

    @BeforeEach
    void pass() throws Exception {
        doAnswer(invocation -> { ((FilterChain) invocation.getArgument(2)).doFilter(invocation.getArgument(0), invocation.getArgument(1)); return null; })
                .when(filter).doFilter(any(), any(), any());
    }

    @Test
    void rejectsBadServiceToken() throws Exception {
        mvc.perform(post("/internal/v1/ingest/events").header("X-Ingest-Service-Token", "bad").contentType("application/json").content(EVENT))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsInvalidGameBeforeReceiptService() throws Exception {
        mvc.perform(post("/internal/v1/ingest/events").header("X-Ingest-Service-Token", "test-token").contentType("application/json")
                        .content(EVENT.replace("cs2", "minecraft")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void exposesSourcePolicyOnlyToTheIngestService() throws Exception {
        when(sourcePolicyService.policy("valorant-official")).thenReturn(new IngestSourcePolicyResponse(false, "PAUSED"));
        mvc.perform(get("/internal/v1/ingest/sources/valorant-official/policy").header("X-Ingest-Service-Token", "test-token"))
                .andExpect(status().isOk()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.enabled").value(false));
        mvc.perform(get("/internal/v1/ingest/sources/valorant-official/policy").header("X-Ingest-Service-Token", "bad"))
                .andExpect(status().isForbidden());
    }

    @Test
    void acceptsServiceTokenAndDelegates() throws Exception {
        when(service.accept(any(), any())).thenReturn(new IngestAcceptance(new IngestReceipt("e1", "h", null, "REVIEW", null), true));
        mvc.perform(post("/internal/v1/ingest/events").header("X-Ingest-Service-Token", "test-token").contentType("application/json").content(EVENT))
                .andExpect(status().isCreated());
        verify(service).accept(argThat(event -> "e1".equals(event.eventId()) && "fps-v1".equals(event.ruleVersion())), any());
    }
    @Test
    void malformedEventsNeverReachReceiptTransaction() throws Exception {
        var json = new com.fasterxml.jackson.databind.ObjectMapper();
        for (String field : java.util.List.of("schemaVersion", "eventId", "gameCodes", "originalUrl", "fetchedAt", "originalPublishedAt")) {
            var event = json.readTree(EVENT);
            var object = (com.fasterxml.jackson.databind.node.ObjectNode) event;
            switch (field) {
                case "schemaVersion" -> object.put(field, 2);
                case "eventId" -> object.put(field, "e".repeat(37));
                case "gameCodes" -> object.putArray(field).addNull();
                case "originalUrl" -> object.put(field, "ftp://example.com/item");
                case "fetchedAt" -> object.put(field, "not-a-date");
                case "originalPublishedAt" -> object.put(field, "2026-09-20T12:00:00");
            }
            mvc.perform(post("/internal/v1/ingest/events").header("X-Ingest-Service-Token", "test-token")
                    .contentType("application/json").content(json.writeValueAsString(event)))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }

    @Test
    void duplicateGamesAndCredentialUrlsAreRejected() throws Exception {
        for (String payload : java.util.List.of(EVENT.replace("[\"cs2\"]", "[\"cs2\",\"cs2\"]"),
                EVENT.replace("https://example.com", "https://user:password@example.com"))) {
            mvc.perform(post("/internal/v1/ingest/events").header("X-Ingest-Service-Token", "test-token")
                    .contentType("application/json").content(payload)).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }
    @Test
    void malformedJsonIs400AndNeverAcknowledged() throws Exception {
        mvc.perform(post("/internal/v1/ingest/events").header("X-Ingest-Service-Token", "test-token")
                .contentType("application/json").content("{invalid-json"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test
    void dateOnlyMetadataDoesNotRequireInventedOffsetTime() throws Exception {
        String payload = EVENT.substring(0, EVENT.length()-1) + ",\"originalPublishedDate\":\"2026-09-20\"}";
        when(service.accept(any(),any())).thenReturn(new IngestAcceptance(new IngestReceipt("e1","h",7,"REVIEW",null),true));
        mvc.perform(post("/internal/v1/ingest/events").header("X-Ingest-Service-Token","test-token")
                .contentType("application/json").content(payload)).andExpect(status().isCreated());
        verify(service).accept(argThat(event -> event.originalPublishedAt()==null && "2026-09-20".equals(event.originalPublishedDate())),any());
    }
    @Test
    void conflictingOrImpossibleDatePrecisionIsRejected() throws Exception {
        for (String extra : java.util.List.of(
                ",\"originalPublishedDate\":\"2026-02-30\"}",
                ",\"originalPublishedDate\":\"2026-09-20\",\"originalPublishedAt\":\"2026-09-20T12:00:00Z\"}")) {
            mvc.perform(post("/internal/v1/ingest/events").header("X-Ingest-Service-Token","test-token")
                    .contentType("application/json").content(EVENT.substring(0,EVENT.length()-1)+extra)).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }
}



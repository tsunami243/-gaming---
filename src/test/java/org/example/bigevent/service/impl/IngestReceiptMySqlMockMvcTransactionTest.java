package org.example.bigevent.service.impl;
import org.example.bigevent.pojo.dto.IngestEvent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.bigevent.BigEventApplication;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.DriverManager;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** MVC filter/controller -> real MySQL proof without opening a Tomcat TCP listener. */
@SpringBootTest(classes = BigEventApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "GAMEBOI_FLYWAY_TEST_URL", matches = "jdbc:mysql://127[.]0[.]0[.]1:33067/gameboi_flyway_test[?]useSSL=false&allowPublicKeyRetrieval=true")
class IngestReceiptMySqlMockMvcTransactionTest {
    private static final String DATABASE = "gameboi_ingest_http_mockmvc_test";
    private static boolean createdDatabase;

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    private final ObjectMapper mapper = new ObjectMapper();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        String url = isolatedUrl();
        createDatabase(url);
        String testUrl = url.replace("gameboi_flyway_test", DATABASE);
        registry.add("spring.datasource.url", () -> testUrl);
        registry.add("spring.datasource.username", () -> "root");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.data.redis.host", () -> "127.0.0.1");
        registry.add("spring.data.redis.port", () -> "6399");
        registry.add("ingest.service-token", () -> "mockmvc-ingest-token");
        registry.add("jwt.secret-key", () -> "mockmvc-ingest-test-secret-key-which-is-long-enough");
        registry.add("ingest.legacy-queue.enabled", () -> "false");
    }

    @AfterAll
    static void dropDatabase() throws Exception {
        if (!createdDatabase) return;
        try (var connection = DriverManager.getConnection(isolatedUrl().replace("gameboi_flyway_test", "mysql"), "root", ""); var statement = connection.createStatement()) {
            statement.execute("DROP DATABASE IF EXISTS " + DATABASE);
        }
    }

    @Test
    void internalHttpReceiptIsIdempotentAndPublishedPublicDtoPreservesSourceAndGames() throws Exception {
        IngestEvent incoming = event("00000000-0000-0000-0000-000000000099", List.of("cs2", "apex-legends"), "https://example.invalid/mockmvc-http");
        String payload = mapper.writeValueAsString(incoming);
        var first = mvc.perform(post("/internal/v1/ingest/events")
                        .header("X-Ingest-Service-Token", "mockmvc-ingest-token")
                        .contentType("application/json").content(payload))
                .andExpect(status().isCreated()).andReturn();
        JsonNode receipt = mapper.readTree(first.getResponse().getContentAsString());
        int articleId = receipt.path("articleId").asInt();
        assertTrue(articleId > 0);
        var replay = mvc.perform(post("/internal/v1/ingest/events")
                        .header("X-Ingest-Service-Token", "mockmvc-ingest-token")
                        .contentType("application/json").content(payload))
                .andExpect(status().isOk()).andReturn();
        assertEquals(articleId, mapper.readTree(replay.getResponse().getContentAsString()).path("articleId").asInt());
        assertEquals(1, jdbc.queryForObject("select count(*) from article where original_url=?", Integer.class, incoming.originalUrl()));
        mvc.perform(get("/api/v1/articles/" + articleId)).andExpect(status().isNotFound());
        jdbc.update("update article set state='已发布',workflow_status='PUBLISHED',published_at='2099-01-01' where id=?", articleId);
        var listed = mvc.perform(get("/api/v1/articles").param("gameCode", "apex-legends").param("contentType", "patch_notes").param("pageSize", "1"))
                .andExpect(status().isOk()).andReturn();
        JsonNode summary = mapper.readTree(listed.getResponse().getContentAsString()).path("data").path("items").get(0);
        assertEquals(articleId, summary.path("id").asInt());
        assertEquals("VALORANT Official Test", summary.path("sourceName").asText());
        assertEquals("cs2", summary.path("gameCodes").get(0).asText());
        assertEquals("apex-legends", summary.path("gameCodes").get(1).asText());
        assertFalse(summary.has("content"));
    }

    private static void createDatabase(String url) {
        try (var connection = DriverManager.getConnection(url.replace("gameboi_flyway_test", "mysql"), "root", ""); var statement = connection.createStatement()) {
            try (var result = statement.executeQuery("select schema_name from information_schema.schemata where schema_name='" + DATABASE + "'")) {
                if (result.next()) throw new IllegalStateException("Refusing to reuse existing disposable database: " + DATABASE);
            }
            statement.execute("CREATE DATABASE " + DATABASE + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            createdDatabase = true;
        } catch (Exception exception) {
            throw new IllegalStateException("Could not create explicit disposable MockMvc ingest database", exception);
        }
    }

    private static String isolatedUrl() {
        String url = System.getenv("GAMEBOI_FLYWAY_TEST_URL");
        if (url == null || !url.matches("jdbc:mysql://127[.]0[.]0[.]1:33067/gameboi_flyway_test[?]useSSL=false&allowPublicKeyRetrieval=true")) {
            throw new IllegalArgumentException("Only explicit 127.0.0.1:33067 disposable MySQL is allowed");
        }
        return url;
    }

    private static IngestEvent event(String eventId, List<String> games, String originalUrl) {
        return new IngestEvent(1, eventId, "valorant-official-test-9", "VALORANT Official Test", "en", games,
                "patch_notes", originalUrl, "无畏契约 13.05 版本更新说明", "英文官方更新已生成中文摘要。", List.of("版本更新"),
                "<p>Staged content only</p>", "REVIEW", "fps-rules-v1", "2026-09-20T12:00:00Z", "2026-09-20T11:00:00Z", null);
    }
}

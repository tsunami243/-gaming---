package org.example.bigevent.service.impl;
import org.example.bigevent.pojo.dto.IngestAcceptance;
import org.example.bigevent.pojo.dto.IngestEvent;
import org.example.bigevent.service.IngestReceiptService;
import org.example.bigevent.service.IngestSourcePolicyService;

import org.example.bigevent.BigEventApplication;
import org.example.bigevent.mapper.ArticleMapper;
import org.example.bigevent.mapper.GameMapper;
import org.example.bigevent.pojo.dto.ArticleGameRow;
import org.example.bigevent.controller.PublicApiV1Controller;
import org.example.bigevent.service.AdminOperationsService;
import org.example.bigevent.pojo.dto.SourcePauseRequest;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.sql.DriverManager;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Runs only against the explicitly configured disposable MySQL test server.
 * It proves the MyBatis writes participate in the service transaction.
 */
@SpringBootTest(classes = BigEventApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfEnvironmentVariable(named = "GAMEBOI_FLYWAY_TEST_URL", matches = ".+")
class IngestReceiptMySqlTransactionTest {
    private static final String DATABASE = "gameboi_ingest_tx_test";
    private static boolean createdDatabase;

    private static String isolatedUrl() {
        String url = System.getenv("GAMEBOI_FLYWAY_TEST_URL");
        if (url == null || !url.matches("jdbc:mysql://(localhost|127[.]0[.]0[.]1):33067/gameboi_flyway_test[?]useSSL=false&allowPublicKeyRetrieval=true")) {
            throw new IllegalArgumentException("Only the explicit disposable localhost:33067 test database is allowed");
        }
        return url;
    }

    @org.springframework.beans.factory.annotation.Value("${local.server.port}") private int httpPort;
    @Autowired private GameMapper gameMapper;
    @Autowired private PublicApiV1Controller publicApi;

    @Autowired
    private IngestReceiptService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private AdminOperationsService adminOperationsService;

    @Autowired
    private IngestSourcePolicyService sourcePolicyService;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        String baseUrl = isolatedUrl();
        String testUrl = baseUrl.replace("gameboi_flyway_test", DATABASE);
        registry.add("server.address", () -> "127.0.0.1");
        registry.add("spring.datasource.url", () -> testUrl + "&createDatabaseIfNotExist=true");
        registry.add("spring.datasource.username", () -> "root");
        registry.add("spring.datasource.password", () -> "");
        registry.add("ingest.service-token", () -> "test-ingest-token");
        registry.add("jwt.secret-key", () -> "ingest-test-secret-key-which-is-long-enough");
        registry.add("spring.data.redis.host", () -> "127.0.0.1");
        registry.add("spring.data.redis.port", () -> "6399");
        registry.add("ingest.legacy-queue.enabled", () -> "false");
    }

    @BeforeAll
    static void migrateDisposableDatabase() throws Exception {
        String baseUrl = isolatedUrl();
        String testUrl = baseUrl.replace("gameboi_flyway_test", DATABASE);
        String adminUrl = baseUrl.replace("gameboi_flyway_test", "mysql");
        try (var connection = DriverManager.getConnection(adminUrl, "root", "");
             var statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE " + DATABASE + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        }
        createdDatabase = true;
        Flyway flyway = Flyway.configure().dataSource(testUrl, "root", "").locations("classpath:db/migration").cleanDisabled(false).load();
        flyway.clean();
        assertEquals(13, flyway.migrate().migrationsExecuted);
    }

    @AfterAll
    static void removeDisposableDatabase() throws Exception {
        if (!createdDatabase) return;
        String baseUrl = isolatedUrl();
        String adminUrl = baseUrl.replace("gameboi_flyway_test", "mysql");
        try (var connection = DriverManager.getConnection(adminUrl, "root", "");
             var statement = connection.createStatement()) {
            statement.execute("DROP DATABASE IF EXISTS " + DATABASE);
        }
    }

    @Test
    void writesReceiptArticleContentAndGamesOnceThenReplaysWithoutDuplicateEffect() {
        IngestEvent event = event("00000000-0000-0000-0000-000000000001", List.of("valorant"), "https://playvalorant.com/en-us/news/game-updates/valorant-patch-notes-13-05");

        IngestAcceptance first = service.accept(event, "a".repeat(64));
        assertTrue(first.created());
        assertNotNull(first.receipt().articleId());
        Integer articleId = first.receipt().articleId();
        assertEquals(1, count("select count(*) from ingest_receipt where event_id=?", event.eventId()));
        assertEquals(1, count("select count(*) from article where id=?", articleId));
        assertEquals(1, count("select count(*) from article_content where article_id=?", articleId));
        assertEquals(1, count("select count(*) from article_game where article_id=?", articleId));
        assertEquals("PENDING_REVIEW", jdbcTemplate.queryForObject("select workflow_status from article where id=?", String.class, articleId));
        assertEquals("2026-09-20 11:00:00", jdbcTemplate.queryForObject("select date_format(original_published_at, '%Y-%m-%d %H:%i:%s') from article where id=?", String.class, articleId));

        jdbcTemplate.update("update article set state='已发布', workflow_status='PUBLISHED', published_at=now() where id=?", articleId);
        assertEquals(articleId, articleMapper.listPublishedByFilters("valorant", "patch_notes").get(0).getId());

        IngestAcceptance replay = service.accept(event, "a".repeat(64));
        assertFalse(replay.created());
        assertEquals(articleId, replay.receipt().articleId());
        assertEquals(1, count("select count(*) from ingest_receipt where event_id=?", event.eventId()));
        assertEquals(1, count("select count(*) from article where canonical_url_hash=(select canonical_url_hash from article where id=?)", articleId));
    }

    @Test
    void adminPauseAndResumeChangesThePolicySeenByTheCrawlerWithoutDatabaseCredentials() {
        assertTrue(sourcePolicyService.policy("valorant-official").enabled());
        adminOperationsService.pause("valorant-official", new SourcePauseRequest("quality review"));
        assertFalse(sourcePolicyService.policy("valorant-official").enabled());
        assertEquals("PAUSED", sourcePolicyService.policy("valorant-official").verificationStatus());
        adminOperationsService.resume("valorant-official");
        assertTrue(sourcePolicyService.policy("valorant-official").enabled());
        assertEquals("SHADOW", sourcePolicyService.policy("valorant-official").verificationStatus());
    }

    @Test
    void rollsBackReceiptSourceArticleAndFirstGameWhenLaterGameMappingFails() {
        IngestEvent event = event("00000000-0000-0000-0000-000000000002", List.of("cs2", "not-enabled"), "https://example.com/cs2/rollback");

        assertThrows(IllegalStateException.class, () -> service.accept(event, "b".repeat(64)));
        assertEquals(0, count("select count(*) from ingest_receipt where event_id=?", event.eventId()));
        assertEquals(0, count("select count(*) from article where original_url=?", event.originalUrl()));
        assertEquals(0, count("select count(*) from content_source where code=?", event.sourceCode()));
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void normalizedGameBatchAndPagedDtoPreserveAllLabels() {
        var event = event("00000000-0000-0000-0000-000000000003", List.of("cs2", "valorant"), "https://example.invalid/batch");
        int id = service.accept(event, "c".repeat(64)).receipt().articleId();
        jdbcTemplate.update("update article set state='已发布', workflow_status='PUBLISHED', category_id=5, published_at='2099-01-01' where id=?", id);
        assertEquals(List.of(new ArticleGameRow(id, "cs2"), new ArticleGameRow(id, "valorant")), gameMapper.findArticleGames(List.of(id)));
        var page = publicApi.articles(1, 1, "cs2", "patch_notes").data();
        assertEquals(1, page.items().size());
        assertEquals(List.of("cs2", "valorant"), page.items().get(0).gameCodes());
        assertEquals(List.of("cs2", "valorant"), publicApi.article(id).data().gameCodes());
        assertNull(com.github.pagehelper.PageHelper.getLocalPage());
    }

    @Test
    void httpReceiptToPublishedSummaryPreservesSourceAndAllGames() throws Exception {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var client = java.net.http.HttpClient.newHttpClient();
        var incoming = event("00000000-0000-0000-0000-000000000004", List.of("cs2", "apex-legends"), "https://example.invalid/http-verified");
        String payload = mapper.writeValueAsString(incoming);
        var post = java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://127.0.0.1:" + httpPort + "/internal/v1/ingest/events"))
                .header("X-Ingest-Service-Token", "test-ingest-token").header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(payload)).build();
        var received = client.send(post, java.net.http.HttpResponse.BodyHandlers.ofString());
        assertEquals(201, received.statusCode(), received.body());
        int id = mapper.readTree(received.body()).path("articleId").asInt();
        assertTrue(id > 0);
        for (int attempt = 0; attempt < 10; attempt++) {
            var replay = client.send(post, java.net.http.HttpResponse.BodyHandlers.ofString());
            assertEquals(200, replay.statusCode(), replay.body());
            assertEquals(id, mapper.readTree(replay.body()).path("articleId").asInt());
        }
        assertEquals(1, count("select count(*) from article where original_url=?", incoming.originalUrl()));
        assertEquals(404, httpGet(client, "/api/v1/articles/" + id).statusCode());
        try {
            // Fixture-only publication; administrator workflow is verified separately.
            jdbcTemplate.update("update article set state='已发布',workflow_status='PUBLISHED',published_at='2099-01-01' where id=?", id);
            var response = httpGet(client, "/api/v1/articles?gameCode=apex-legends&contentType=patch_notes&pageSize=1");
            assertEquals(200, response.statusCode(), response.body());
            var summary = mapper.readTree(response.body()).path("data").path("items").get(0);
            assertEquals(id, summary.path("id").asInt());
            assertEquals("VALORANT Official Test", summary.path("sourceName").asText());
            assertEquals("cs2", summary.path("gameCodes").get(0).asText());
            assertEquals("apex-legends", summary.path("gameCodes").get(1).asText());
            assertEquals(incoming.summary(), summary.path("summary").asText());
            assertFalse(summary.has("content"));
            var detail = httpGet(client, "/api/v1/articles/" + id);
            assertEquals(200, detail.statusCode(), detail.body());
            var data = mapper.readTree(detail.body()).path("data");
            assertEquals(summary.path("sourceName"), data.path("sourceName"));
            assertEquals(incoming.originalUrl(), data.path("originalUrl").asText());
            // FULL_TEXT reading mode now serves the stored body through the detail endpoint,
            // converted to plain text so the public page never renders stored markup.
            assertEquals("FULL_TEXT", data.path("readingMode").asText());
            assertEquals("<p>Staged content only</p>", incoming.content());
            assertEquals("Staged content only", data.path("content").asText());
            if ("1".equals(System.getenv("GAMEBOI_BROWSER_E2E"))) {
                var log = java.nio.file.Path.of("target", "live-web-current.log");
                var browser = new ProcessBuilder("python", "scripts/check-web-live.py", "--port", String.valueOf(httpPort), "--article-id", String.valueOf(id))
                        .redirectErrorStream(true).redirectOutput(log.toFile()).start();
                try {
                    assertTrue(browser.waitFor(90, java.util.concurrent.TimeUnit.SECONDS), "Browser test timed out");
                    assertEquals(0, browser.exitValue(), java.nio.file.Files.readString(log));
                } finally {
                    if (browser.isAlive()) browser.destroyForcibly();
                }
            }
        } finally {
            jdbcTemplate.update("update article set state='草稿',workflow_status='UNPUBLISHED' where id=?", id);
        }
        assertEquals(404, httpGet(client, "/api/v1/articles/" + id).statusCode());
    }

    private java.net.http.HttpResponse<String> httpGet(java.net.http.HttpClient client, String path) throws Exception {
        var request = java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://127.0.0.1:" + httpPort + path)).GET().build();
        return client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
    }

    private int count(String sql, Object... args) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }

    private static IngestEvent event(String eventId, List<String> games, String originalUrl) {
        return new IngestEvent(1, eventId, "valorant-official-test-" + eventId.substring(eventId.length() - 1), "VALORANT Official Test", "en", games,
                "patch_notes", originalUrl, "无畏契约 13.05 版本更新说明", "英文官方更新已生成中文摘要。", List.of("版本更新"),
                "<p>Staged content only</p>", "REVIEW", "fps-rules-v1", "2026-09-20T12:00:00Z", "2026-09-20T11:00:00Z", null);
    }
}







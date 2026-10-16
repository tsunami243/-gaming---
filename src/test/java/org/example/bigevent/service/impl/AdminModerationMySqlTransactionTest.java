package org.example.bigevent.service.impl;
import org.example.bigevent.pojo.dto.ModerationAuditRow;
import org.example.bigevent.pojo.dto.ModerationRequest;
import org.example.bigevent.service.AdminModerationService;

import org.example.bigevent.BigEventApplication;
import org.example.bigevent.utils.ThreadLocalUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Real MySQL proof for the moderation transition/audit pair. It can only use the explicit
 * disposable 127.0.0.1:33067 test server; it never accepts a general local or remote JDBC URL.
 */
@SpringBootTest(classes = BigEventApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EnabledIfEnvironmentVariable(named = "GAMEBOI_FLYWAY_TEST_URL", matches = "jdbc:mysql://127[.]0[.]0[.]1:33067/gameboi_flyway_test[?]useSSL=false&allowPublicKeyRetrieval=true")
class AdminModerationMySqlTransactionTest {
    private static final String DATABASE = "gameboi_admin_moderation_test";
    private static boolean createdDatabase;

    @Autowired AdminModerationService service;
    @Autowired JdbcTemplate jdbc;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        String url = isolatedUrl();
        createDisposableDatabase(url);
        String testUrl = url.replace("gameboi_flyway_test", DATABASE);
        registry.add("spring.datasource.url", () -> testUrl);
        registry.add("spring.datasource.username", () -> "root");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.data.redis.host", () -> "127.0.0.1");
        registry.add("spring.data.redis.port", () -> "6399");
        registry.add("jwt.secret-key", () -> "admin-mysql-test-secret-key-which-is-long-enough");
        registry.add("ingest.service-token", () -> "admin-mysql-test-ingest-token");
        registry.add("ingest.legacy-queue.enabled", () -> "false");
    }

    @AfterEach void clearClaims() { ThreadLocalUtil.remove(); }

    @AfterAll
    static void removeDisposableDatabase() throws Exception {
        if (!createdDatabase) return;
        String adminUrl = isolatedUrl().replace("gameboi_flyway_test", "mysql");
        try (var connection = DriverManager.getConnection(adminUrl, "root", ""); var statement = connection.createStatement()) {
            statement.execute("DROP DATABASE IF EXISTS " + DATABASE);
        }
    }

    @Test
    void approvesAndReadsBackTheSameActorThroughTheActualMySqlMapper() {
        int articleId = pendingArticle("mysql audit happy path");
        ThreadLocalUtil.set(Map.of("userId", 1));

        var changed = service.approve(articleId, new ModerationRequest(0, "OFFICIAL", "official patch notes verified"));
        var audit = service.audit(articleId, 1, 20);

        assertEquals("PUBLISHED", changed.getWorkflowStatus());
        assertEquals(1, changed.getRevision());
        assertEquals(1, audit.total());
        ModerationAuditRow entry = audit.items().get(0);
        assertEquals(1, entry.actorUserId());
        assertEquals("APPROVE", entry.action());
        assertEquals("PENDING_REVIEW", entry.beforeStatus());
        assertEquals("PUBLISHED", entry.afterStatus());
        assertEquals(1, entry.revision());
        assertEquals("official patch notes verified", entry.reasonDetail());
    }

    @Test
    void auditInsertFailureRollsBackTheArticleTransitionInTheSameDatabaseTransaction() {
        int articleId = pendingArticle("mysql audit rollback");
        ThreadLocalUtil.set(Map.of("userId", 1));

        assertThrows(Exception.class, () -> service.approve(articleId, new ModerationRequest(0, "OFFICIAL", "x".repeat(1001))));

        assertEquals("PENDING_REVIEW", jdbc.queryForObject("select workflow_status from article where id=?", String.class, articleId));
        assertEquals(0, jdbc.queryForObject("select revision from article where id=?", Integer.class, articleId));
        assertEquals(0, jdbc.queryForObject("select count(*) from moderation_audit where article_id=?", Integer.class, articleId));
    }

    private int pendingArticle(String title) {
        Integer categoryId = jdbc.queryForObject("select id from category order by id limit 1", Integer.class);
        Integer userId = jdbc.queryForObject("select id from `user` where username='ingest-system'", Integer.class);
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement("insert into article(title,content,state,category_id,create_user,workflow_status,revision) values(?,?,?,?,?,'PENDING_REVIEW',0)", Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, title);
            statement.setString(2, "test-only article body");
            statement.setString(3, "草稿");
            statement.setInt(4, categoryId);
            statement.setInt(5, userId);
            return statement;
        }, keys);
        assertNotNull(keys.getKey());
        return keys.getKey().intValue();
    }

    private static void createDisposableDatabase(String url) {
        String adminUrl = url.replace("gameboi_flyway_test", "mysql");
        try (var connection = DriverManager.getConnection(adminUrl, "root", ""); var statement = connection.createStatement()) {
            try (var result = statement.executeQuery("select schema_name from information_schema.schemata where schema_name='" + DATABASE + "'")) {
                if (result.next()) throw new IllegalStateException("Refusing to reuse an existing disposable database: " + DATABASE);
            }
            statement.execute("CREATE DATABASE " + DATABASE + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            createdDatabase = true;
        } catch (Exception e) {
            throw new IllegalStateException("Could not create the explicitly named disposable moderation database", e);
        }
    }

    private static String isolatedUrl() {
        String url = System.getenv("GAMEBOI_FLYWAY_TEST_URL");
        if (url == null || !url.matches("jdbc:mysql://127[.]0[.]0[.]1:33067/gameboi_flyway_test[?]useSSL=false&allowPublicKeyRetrieval=true")) {
            throw new IllegalArgumentException("Only the explicit disposable localhost:33067 test database is allowed");
        }
        return url;
    }
}

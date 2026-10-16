package org.example.bigevent.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.assertNull;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class MySqlFlywayIntegrationTest {

    @Test
    void migratesAnEmptyMysqlDatabaseAndIsIdempotentOnSecondRun() throws Exception {
        String url = isolatedUrl();

        Flyway flyway = Flyway.configure()
                .dataSource(url, "root", "")
                .locations("classpath:db/migration")
                .cleanDisabled(false)
                .load();

        flyway.clean();
        assertEquals(13, flyway.migrate().migrationsExecuted);
        assertEquals(0, flyway.migrate().migrationsExecuted);

        try (var connection = DriverManager.getConnection(url, "root", "");
             var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT COUNT(*) FROM game WHERE enabled = 1")) {
            result.next();
            assertEquals(4, result.getInt(1));
        }
        try (var connection = DriverManager.getConnection(url, "root", "");
             var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT role FROM `user` WHERE username = 'ingest-system'")) {
            result.next();
            assertEquals("ROLE_CONTENT_ADMIN", result.getString(1));
        }
        try (var connection = DriverManager.getConnection(url, "root", "");
             var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT verification_status, auto_publish_enabled FROM content_source WHERE code = 'valorant-official'")) {
            result.next();
            assertEquals("SHADOW", result.getString(1));
            assertEquals(0, result.getInt(2));
        }
        try (var connection = DriverManager.getConnection(url, "root", "");
             var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT data_type, is_nullable FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'article' AND column_name = 'original_published_date'")) {
            result.next();
            assertEquals("date", result.getString("data_type"));
            assertEquals("YES", result.getString("is_nullable"));
        }
        try (var connection = DriverManager.getConnection(url, "root", "");
             var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT verification_status, auto_publish_enabled FROM content_source WHERE code = 'delta-force-garena-official'")) {
            result.next();
            // Promoted to SHADOW by V12 so the worker may run it; automatic publication stays off.
            assertEquals("SHADOW", result.getString(1));
            assertEquals(0, result.getInt(2));
        }
        try (var connection = DriverManager.getConnection(url, "root", "");
             var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT verification_status, auto_publish_enabled FROM content_source WHERE code = 'cs2-official'")) {
            result.next();
            assertEquals("FETCH_VERIFIED", result.getString(1));
            assertEquals(0, result.getInt(2));
        }
        try (var connection = DriverManager.getConnection(url, "root", "");
             var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT verification_status, auto_publish_enabled FROM content_source WHERE code = 'valorant-esports-official'")) {
            result.next();
            assertEquals("FETCH_VERIFIED", result.getString(1));
            assertEquals(0, result.getInt(2));
        }
        try (var connection = DriverManager.getConnection(url, "root", "");
             var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT verification_status, auto_publish_enabled FROM content_source WHERE code = 'algs-official'")) {
            result.next();
            assertEquals("FETCH_VERIFIED", result.getString(1));
            assertEquals(0, result.getInt(2));
        }
    }

    @Test
    void upgradesASeededLegacyCopyWithoutChangingItsArticleIdentity() throws Exception {
        String url = isolatedUrl();
        String legacyUrl = url.replace("gameboi_flyway_test", "gameboi_flyway_legacy_test");
        assumeTrue(!legacyUrl.equals(url), "test URL database must be named gameboi_flyway_test");

        try (var rootConnection = DriverManager.getConnection(url, "root", "");
             var statement = rootConnection.createStatement()) {
            statement.execute("DROP DATABASE IF EXISTS gameboi_flyway_legacy_test");
            statement.execute("CREATE DATABASE gameboi_flyway_legacy_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        }

        Flyway legacyV1 = Flyway.configure()
                .dataSource(legacyUrl, "root", "")
                .locations("classpath:db/migration")
                .target("1")
                .load();
        assertEquals(1, legacyV1.migrate().migrationsExecuted);

        try (var connection = DriverManager.getConnection(legacyUrl, "root", "");
             var statement = connection.createStatement()) {
            statement.execute("INSERT INTO category (id, category_name, category_alias) VALUES (9, '旧 Counter-Strike', 'legacy-cs')");
            statement.execute("INSERT INTO `user` (id, username, password) VALUES (1, 'legacy-editor', 'not-a-real-password')");
            statement.execute("INSERT INTO article (id, title, content, state, category_id, create_user) VALUES (101, 'Legacy CS2 note', 'legacy content', '草稿', 9, 1)");
            statement.execute("INSERT INTO article (id, title, content, state, category_id, create_user) VALUES (102, 'Legacy unknown category', 'legacy content', '草稿', 77, 1)");
            statement.execute("INSERT INTO comment (id, article_id, user_id, content) VALUES (201, 101, 1, 'legacy comment')");
            statement.execute("INSERT INTO article_like (id, article_id, user_id) VALUES (301, 101, 1)");
            statement.execute("INSERT INTO article_favorite (id, article_id, user_id) VALUES (401, 101, 1)");
        }

        Flyway full = Flyway.configure()
                .dataSource(legacyUrl, "root", "")
                .locations("classpath:db/migration")
                .load();
        assertEquals(12, full.migrate().migrationsExecuted);

        try (var connection = DriverManager.getConnection(legacyUrl, "root", "");
             var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT id, category_id, workflow_status, source_id, summary FROM article WHERE id = 101")) {
            result.next();
            assertEquals(101, result.getInt("id"));
            assertEquals(9, result.getInt("category_id"));
            assertEquals("DRAFT", result.getString("workflow_status"));
            assertNull(result.getObject("source_id"));
            assertNull(result.getString("summary"));
        }

        try (var connection = DriverManager.getConnection(legacyUrl, "root", "");
             var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT (SELECT COUNT(*) FROM comment WHERE id = 201 AND article_id = 101) + "
                     + "(SELECT COUNT(*) FROM article_like WHERE id = 301 AND article_id = 101) + "
                     + "(SELECT COUNT(*) FROM article_favorite WHERE id = 401 AND article_id = 101)")) {
            result.next();
            assertEquals(3, result.getInt(1));
        }

        try (var connection = DriverManager.getConnection(legacyUrl, "root", "");
             var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT legacy_category_id, reason_code FROM legacy_article_review WHERE article_id = 102")) {
            result.next();
            assertEquals(77, result.getInt("legacy_category_id"));
            assertEquals("UNMAPPED_LEGACY_CATEGORY", result.getString("reason_code"));
        }
    }

    private String isolatedUrl() {
        String url = System.getenv("GAMEBOI_FLYWAY_TEST_URL");
        assumeTrue(url != null && !url.isBlank(), "set GAMEBOI_FLYWAY_TEST_URL to run against an isolated MySQL instance");
        boolean localHost = url.startsWith("jdbc:mysql://localhost:") || url.startsWith("jdbc:mysql://127.0.0.1:");
        assumeTrue(localHost && url.contains("/gameboi_flyway_test"),
                "integration test only accepts a localhost gameboi_flyway_test database");
        return url;
    }

}






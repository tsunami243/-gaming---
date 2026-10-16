package org.example.bigevent.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlywayMigrationLayoutTest {

    @Test
    void requiredFlywayResourcesAndFirstReleaseSeedsExist() throws IOException {
        assertNotNull(Flyway.class, "Flyway must remain on the runtime classpath");

        String v1 = resource("db/migration/V1__legacy_core_schema.sql");
        String v2 = resource("db/migration/V2__first_release_content_model.sql");
        String v3 = resource("db/migration/V3__seed_first_release_games.sql");
        String v4 = resource("db/migration/V4__seed_ingest_system_user.sql");
        String v5 = resource("db/migration/V5__seed_verified_valorant_source.sql");
        String v6 = resource("db/migration/V6__seed_verified_apex_source.sql");
        String v8 = resource("db/migration/V8__seed_fetch_verified_delta_force_garena_source.sql");
        String v9 = resource("db/migration/V9__seed_fetch_verified_cs2_steam_source.sql");
        String v10 = resource("db/migration/V10__seed_fetch_verified_valorant_esports_source.sql");
        String v11 = resource("db/migration/V11__seed_fetch_verified_algs_source.sql");
        String v12 = resource("db/migration/V12__promote_delta_force_source_to_shadow.sql");
        String v13 = resource("db/migration/V13__align_delta_force_source_identity.sql");

        assertTrue(v1.contains("CREATE TABLE `user`"));
        assertTrue(v1.contains("CREATE TABLE article"));
        assertTrue(v1.contains("CREATE TABLE comment"));
        assertTrue(v2.contains("CREATE TABLE game"));
        assertTrue(v2.contains("CREATE TABLE content_source"));
        assertTrue(v2.contains("CREATE TABLE ingest_receipt"));
        assertTrue(v2.contains("CREATE TABLE ingest_run"));
        assertTrue(v2.contains("CREATE TABLE moderation_audit"));
        assertTrue(v2.contains("CREATE TABLE legacy_article_review"));
        assertTrue(v2.contains("ADD COLUMN summary"));
        assertTrue(v3.contains("'delta-force'"));
        assertTrue(v3.contains("'valorant'"));
        assertTrue(v3.contains("'cs2'"));
        assertTrue(v3.contains("'apex-legends'"));
        assertTrue(v4.contains("'ingest-system'"));
        assertTrue(v5.contains("'valorant-official'"));
        assertTrue(v6.contains("'apex-official'"));
        assertTrue(v8.contains("'delta-force-garena-official'"));
        assertTrue(v8.contains("'FETCH_VERIFIED'"));
        assertTrue(v9.contains("'cs2-official'"));
        assertTrue(v9.contains("'FETCH_VERIFIED'"));
        assertTrue(v10.contains("'valorant-esports-official'"));
        assertTrue(v10.contains("'FETCH_VERIFIED'"));
        assertTrue(v11.contains("'algs-official'"));
        assertTrue(v11.contains("'FETCH_VERIFIED'"));
        assertTrue(v12.contains("'delta-force-garena-official'"));
        assertTrue(v12.contains("verification_status = 'SHADOW'"));
        // Promotion must never switch automatic publication on.
        assertTrue(v12.contains("auto_publish_enabled = 0"));
        assertTrue(v13.contains("'delta-force-garena-official'"));
        assertTrue(v13.contains("df.qq.com"));
        // The published source code is intentionally preserved by this migration.
        assertFalse(v13.contains("SET code"));
    }

    @Test
    void migrationFilesHaveContiguousVersionsAndNoCredentials() throws IOException {
        String[] names = {
                "V1__legacy_core_schema.sql",
                "V2__first_release_content_model.sql",
                "V3__seed_first_release_games.sql",
                "V4__seed_ingest_system_user.sql",
                "V5__seed_verified_valorant_source.sql",
                "V6__seed_verified_apex_source.sql",
                "V7__source_publication_date_precision.sql",
                "V8__seed_fetch_verified_delta_force_garena_source.sql",
                "V9__seed_fetch_verified_cs2_steam_source.sql",
                "V10__seed_fetch_verified_valorant_esports_source.sql",
                "V11__seed_fetch_verified_algs_source.sql",
                "V12__promote_delta_force_source_to_shadow.sql",
                "V13__align_delta_force_source_identity.sql"
        };
        for (int i = 0; i < names.length; i++) {
            assertTrue(names[i].startsWith("V" + (i + 1) + "__"));
            String sql = resource("db/migration/" + names[i]).toLowerCase();
            assertFalse(sql.contains("password="));
            assertFalse(sql.contains("secret="));
        }
        assertEquals(13, names.length);
    }

    private String resource(String path) throws IOException {
        try (var input = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(input, () -> "Missing migration resource: " + path);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

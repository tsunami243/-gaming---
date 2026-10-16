# Database migrations and local-only configuration

## Migration tool

The project uses **Flyway** through `flyway-core` and `flyway-mysql`. The Spring Boot parent is pinned to `4.0.5`; the resolved Flyway version in this workspace is `11.14.1`. Migrations live in:

```text
src/main/resources/db/migration/
```

| Version | Purpose |
|---|---|
| V1 | Complete empty-database baseline for legacy users, categories, articles, interactions, views, roles and the disabled push demo table. |
| V2 | First-release FPS model: games, sources, article-source/game/content relations, ingestion receipt/run/record tables, moderation audit and article metadata/workflow fields. |
| V3 | Seeds the four allowed games and compatibility categories required by existing code. |

Flyway records successful versions in `flyway_schema_history`; running the application or `migrate` again does not rerun seed inserts or DDL.

## Configuration

The repository contains no database password or JWT secret. Before running the backend locally, set values in the current PowerShell session:

```powershell
$env:DB_USERNAME = 'your-local-database-user'
$env:DB_PASSWORD = 'your-local-database-password'
$env:JWT_SECRET_KEY = 'a-long-random-local-development-secret'
```

The database URL remains in `application.yml` only as a local development default. Production Compose configuration will use a separate environment file that is never committed.

## Empty database validation

The migration resource layout test runs with the normal test suite. A true MySQL test is intentionally opt-in and needs an **isolated** database URL; it is never pointed at the existing local MySQL service or the server database:

```powershell
$env:GAMEBOI_FLYWAY_TEST_URL = 'jdbc:mysql://127.0.0.1:33067/gameboi_flyway_test?useSSL=false&allowPublicKeyRetrieval=true'
.\mvnw.cmd test '-Dtest=MySqlFlywayIntegrationTest'
```

The test refuses non-localhost URLs and requires the exact `gameboi_flyway_test` database name before it invokes Flyway clean. It applies V1–V11, applies them a second time, verifies the four enabled game rows, the ingest-system account, the VALORANT SHADOW source, and the nullable `article.original_published_date` DATE column. It then upgrades a seeded V1 legacy copy and verifies that its article ID, category, comment, like and favorite relationships remain intact. It also verifies that an unknown legacy category ID is added to `legacy_article_review`. Do not change these safety checks to point at user or production data.

## Legacy database preflight and upgrade plan

**Do not run Flyway automatically against an existing `big_event` database.** Existing deployments predate the migration baseline and have incomplete/unknown table history.

1. Obtain a read-only schema and row-count export from the old database; never put it in Git.
2. Create and restore a disposable copy of that export on an isolated MySQL instance.
3. Record the table/column inventory and legacy category IDs. Map only these known IDs: `5 -> delta-force`, `8 -> valorant`, `9 -> cs2`, `10 -> apex-legends`.
4. V2 writes articles with category IDs other than 5/8/9/10 into `legacy_article_review` using `UNMAPPED_LEGACY_CATEGORY`; missing source data remains null and ambiguous state remains draft. Do not delete or auto-publish these records.
5. Write a dedicated, reviewed legacy import migration for the observed schema. It must preserve old primary keys and references, backfill `workflow_status`, and set source-related fields to null/legacy markers where evidence is unavailable.
6. Run the import only on the disposable copy, then compare row counts, foreign references, published/draft counts and four-game mappings before requesting permission for a production migration.
7. Production migration requires a new approval, a tested backup, a migration window and a rollback plan. This task did not receive that approval and did not connect to an existing database.

## Current limitations

- The current Article mapper continues to write legacy columns; API-01/DATA follow-up work will introduce DTOs and persistence methods for V2 fields.
- On 2026-09-22, `MySqlFlywayIntegrationTest` was run against a new, separate MySQL 8.0.45 process bound only to `127.0.0.1:33067`, with a distinct data directory and 128 MiB InnoDB buffer pool. It applied V1–V11 twice, verified V7 `original_published_date` is a nullable DATE plus the V8 Delta Force Garena, V9 CS2 Steam, V10 VALORANT Esports and V11 ALGS FETCH_VERIFIED seeds, and upgraded a seeded V1 legacy copy. `AdminModerationMySqlTransactionTest` used a separately created/dropped test database on the same isolated instance to prove real approve→audit persistence and rollback when audit insertion exceeds the database bound. The temporary server was stopped afterward; the existing MySQL service on port 3306 was not contacted.
- Full old-database upgrade evidence is unavailable until a user-approved, sanitized schema copy is provided.





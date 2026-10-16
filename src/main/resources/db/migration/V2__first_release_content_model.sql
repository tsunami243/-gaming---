-- First-release FPS content model. Existing legacy article fields remain during the API migration.

CREATE TABLE game (
    id INT NOT NULL AUTO_INCREMENT,
    code VARCHAR(64) NOT NULL,
    name_zh VARCHAR(100) NOT NULL,
    name_original VARCHAR(150) NOT NULL,
    aliases JSON NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_game_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE content_source (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(200) NOT NULL,
    source_type VARCHAR(20) NOT NULL,
    base_url VARCHAR(2048) NOT NULL,
    allowed_domains JSON NOT NULL,
    game_scope JSON NOT NULL,
    language VARCHAR(16) NOT NULL,
    adapter_key VARCHAR(100) NOT NULL,
    schedule_hours SMALLINT NOT NULL,
    candidate_limit SMALLINT NOT NULL,
    auto_publish_enabled TINYINT NOT NULL DEFAULT 0,
    verification_status VARCHAR(20) NOT NULL DEFAULT 'CANDIDATE',
    last_verified_at DATETIME NULL,
    last_success_at DATETIME NULL,
    paused_at DATETIME NULL,
    pause_reason VARCHAR(500) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_content_source_code (code),
    KEY idx_content_source_status (verification_status, auto_publish_enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE article
    ADD COLUMN summary TEXT NULL AFTER title,
    ADD COLUMN source_id BIGINT NULL AFTER category_id,
    ADD COLUMN original_url VARCHAR(2048) NULL AFTER source_id,
    ADD COLUMN canonical_url_hash CHAR(64) NULL AFTER original_url,
    ADD COLUMN original_title VARCHAR(500) NULL AFTER canonical_url_hash,
    ADD COLUMN source_language VARCHAR(16) NULL AFTER original_title,
    ADD COLUMN original_published_at DATETIME NULL AFTER source_language,
    ADD COLUMN ingested_at DATETIME NULL AFTER original_published_at,
    ADD COLUMN published_at DATETIME NULL AFTER ingested_at,
    ADD COLUMN content_type VARCHAR(32) NULL AFTER published_at,
    ADD COLUMN reading_mode VARCHAR(32) NOT NULL DEFAULT 'EXTERNAL_SUMMARY' AFTER content_type,
    ADD COLUMN workflow_status VARCHAR(32) NOT NULL DEFAULT 'DRAFT' AFTER reading_mode,
    ADD COLUMN workflow_reason_code VARCHAR(64) NULL AFTER workflow_status,
    ADD COLUMN revision INT NOT NULL DEFAULT 0 AFTER workflow_reason_code;

ALTER TABLE article
    ADD CONSTRAINT fk_article_source FOREIGN KEY (source_id) REFERENCES content_source(id),
    ADD UNIQUE KEY uk_article_canonical_url_hash (canonical_url_hash),
    ADD KEY idx_article_workflow_published (workflow_status, published_at),
    ADD KEY idx_article_source_original_published (source_id, original_published_at);

CREATE TABLE article_game (
    article_id INT NOT NULL,
    game_id INT NOT NULL,
    is_primary TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (article_id, game_id),
    KEY idx_article_game_game (game_id, article_id),
    CONSTRAINT fk_article_game_article FOREIGN KEY (article_id) REFERENCES article(id) ON DELETE CASCADE,
    CONSTRAINT fk_article_game_game FOREIGN KEY (game_id) REFERENCES game(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE article_content (
    article_id INT NOT NULL,
    sanitized_html MEDIUMTEXT NULL,
    content_hash CHAR(64) NULL,
    extractor_version VARCHAR(50) NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (article_id),
    KEY idx_article_content_hash (content_hash),
    CONSTRAINT fk_article_content_article FOREIGN KEY (article_id) REFERENCES article(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ingest_run (
    run_id CHAR(36) NOT NULL,
    source_id BIGINT NULL,
    status VARCHAR(20) NOT NULL,
    started_at DATETIME NOT NULL,
    finished_at DATETIME NULL,
    discovered_count INT NOT NULL DEFAULT 0,
    processed_count INT NOT NULL DEFAULT 0,
    duplicate_count INT NOT NULL DEFAULT 0,
    review_count INT NOT NULL DEFAULT 0,
    rejected_count INT NOT NULL DEFAULT 0,
    failed_count INT NOT NULL DEFAULT 0,
    daily_candidate_budget_used INT NOT NULL DEFAULT 0,
    paused_reason VARCHAR(500) NULL,
    PRIMARY KEY (run_id),
    KEY idx_ingest_run_source_started (source_id, started_at),
    CONSTRAINT fk_ingest_run_source FOREIGN KEY (source_id) REFERENCES content_source(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ingest_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    source_id BIGINT NOT NULL,
    run_id CHAR(36) NULL,
    original_url VARCHAR(2048) NOT NULL,
    canonical_url_hash CHAR(64) NOT NULL,
    content_hash CHAR(64) NULL,
    processing_stage VARCHAR(32) NOT NULL,
    decision VARCHAR(16) NULL,
    reason_code VARCHAR(64) NULL,
    rule_version VARCHAR(50) NOT NULL,
    prompt_version VARCHAR(50) NULL,
    extractor_version VARCHAR(50) NOT NULL,
    article_id INT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    last_error_code VARCHAR(64) NULL,
    last_error_summary VARCHAR(500) NULL,
    first_seen_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ingest_record_source_url (source_id, canonical_url_hash),
    KEY idx_ingest_record_stage_seen (processing_stage, last_seen_at),
    KEY idx_ingest_record_article (article_id),
    CONSTRAINT fk_ingest_record_source FOREIGN KEY (source_id) REFERENCES content_source(id),
    CONSTRAINT fk_ingest_record_run FOREIGN KEY (run_id) REFERENCES ingest_run(run_id),
    CONSTRAINT fk_ingest_record_article FOREIGN KEY (article_id) REFERENCES article(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ingest_receipt (
    event_id CHAR(36) NOT NULL,
    payload_hash CHAR(64) NOT NULL,
    article_id INT NULL,
    final_decision VARCHAR(16) NOT NULL,
    rule_version VARCHAR(50) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (event_id),
    KEY idx_ingest_receipt_created (created_at),
    CONSTRAINT fk_ingest_receipt_article FOREIGN KEY (article_id) REFERENCES article(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE legacy_article_review (
    article_id INT NOT NULL,
    legacy_category_id INT NULL,
    reason_code VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (article_id),
    KEY idx_legacy_article_review_reason (reason_code),
    CONSTRAINT fk_legacy_article_review_article FOREIGN KEY (article_id) REFERENCES article(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE moderation_audit (
    id BIGINT NOT NULL AUTO_INCREMENT,
    article_id INT NOT NULL,
    actor_user_id INT NULL,
    action VARCHAR(32) NOT NULL,
    reason_code VARCHAR(64) NULL,
    reason_detail VARCHAR(1000) NULL,
    before_status VARCHAR(32) NULL,
    after_status VARCHAR(32) NOT NULL,
    revision INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_moderation_audit_article_created (article_id, created_at),
    CONSTRAINT fk_moderation_audit_article FOREIGN KEY (article_id) REFERENCES article(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO legacy_article_review (article_id, legacy_category_id, reason_code)
SELECT id, category_id, 'UNMAPPED_LEGACY_CATEGORY'
FROM article
WHERE category_id NOT IN (5, 8, 9, 10);

UPDATE article
SET workflow_status = CASE state
    WHEN '已发布' THEN 'PUBLISHED'
    WHEN '草稿' THEN 'DRAFT'
    ELSE 'DRAFT'
END,
published_at = CASE WHEN state = '已发布' THEN update_time ELSE NULL END;

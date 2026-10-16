-- Flyway baseline for a new, empty MySQL 8 database.
-- This recreates the legacy application tables before V2 adds the first-release content model.

CREATE TABLE `user` (
    id INT NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50) NULL,
    email VARCHAR(128) NULL,
    user_pic VARCHAR(2048) NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'ROLE_USER',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE category (
    id INT NOT NULL AUTO_INCREMENT,
    category_name VARCHAR(100) NOT NULL,
    category_alias VARCHAR(100) NOT NULL,
    create_user INT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_category_alias (category_alias)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE article (
    id INT NOT NULL AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    content MEDIUMTEXT NOT NULL,
    cover_img VARCHAR(2048) NOT NULL DEFAULT '',
    state VARCHAR(20) NOT NULL DEFAULT '草稿',
    category_id INT NOT NULL,
    create_user INT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    views INT NOT NULL DEFAULT 0,
    unique_views INT NOT NULL DEFAULT 0,
    favorites INT NOT NULL DEFAULT 0,
    tags JSON NULL,
    is_carousel TINYINT NOT NULL DEFAULT 0,
    heat INT NOT NULL DEFAULT 0,
    heat_updated_at DATETIME NULL,
    PRIMARY KEY (id),
    KEY idx_article_state_created (state, create_time),
    KEY idx_article_category_state (category_id, state),
    KEY idx_article_create_user (create_user)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE comment (
    id INT NOT NULL AUTO_INCREMENT,
    article_id INT NOT NULL,
    user_id INT NOT NULL,
    user_name VARCHAR(50) NULL,
    user_avatar VARCHAR(2048) NULL,
    content TEXT NOT NULL,
    parent_id INT NULL,
    reply_id INT NULL,
    reply_user_name VARCHAR(50) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    like_count INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_comment_article_created (article_id, create_time),
    KEY idx_comment_parent (parent_id),
    KEY idx_comment_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE article_like (
    id INT NOT NULL AUTO_INCREMENT,
    article_id INT NOT NULL,
    user_id INT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_article_like (article_id, user_id),
    KEY idx_article_like_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE comment_like (
    id INT NOT NULL AUTO_INCREMENT,
    comment_id INT NOT NULL,
    user_id INT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_comment_like (comment_id, user_id),
    KEY idx_comment_like_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE article_favorite (
    id INT NOT NULL AUTO_INCREMENT,
    article_id INT NOT NULL,
    user_id INT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_article_favorite (article_id, user_id),
    KEY idx_article_favorite_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE article_view_history (
    id INT NOT NULL AUTO_INCREMENT,
    article_id INT NOT NULL,
    user_id INT NULL,
    ip_address VARCHAR(50) NULL,
    view_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_article_view_article (article_id),
    KEY idx_article_view_user (user_id),
    KEY idx_article_view_ip (article_id, ip_address, view_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE role (
    id INT NOT NULL AUTO_INCREMENT,
    role_name VARCHAR(50) NOT NULL,
    role_alias VARCHAR(50) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_alias (role_alias)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_role (
    id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    role_id INT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE push_task (
    id BIGINT NOT NULL AUTO_INCREMENT,
    message_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    content VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    last_error VARCHAR(500) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_push_task_message (message_id),
    KEY idx_push_task_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO role (role_name, role_alias) VALUES
    ('普通用户', 'ROLE_USER'),
    ('内容管理员', 'ROLE_CONTENT_ADMIN'),
    ('超级管理员', 'ROLE_SUPER_ADMIN');

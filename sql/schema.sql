-- ============================================
-- 新增数据库表结构
-- ============================================

-- 1. 文章浏览记录表
CREATE TABLE IF NOT EXISTS `article_view_history` (
  `id` int NOT NULL AUTO_INCREMENT,
  `article_id` int NOT NULL COMMENT '文章ID',
  `user_id` int DEFAULT NULL COMMENT '用户ID，游客为null',
  `ip_address` varchar(50) DEFAULT NULL COMMENT 'IP地址，用于游客去重',
  `view_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '浏览时间',
  PRIMARY KEY (`id`),
  KEY `idx_article_id` (`article_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章浏览记录表';

-- 2. 文章收藏表
CREATE TABLE IF NOT EXISTS `article_favorite` (
  `id` int NOT NULL AUTO_INCREMENT,
  `article_id` int NOT NULL COMMENT '文章ID',
  `user_id` int NOT NULL COMMENT '用户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_article_user` (`article_id`, `user_id`) COMMENT '防止重复收藏',
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章收藏表';

-- ============================================
-- 为article表新增字段（如果尚未添加）
-- ============================================
-- ALTER TABLE `article` ADD COLUMN `views` int DEFAULT 0 COMMENT '浏览量' AFTER `update_time`;
-- ALTER TABLE `article` ADD COLUMN `unique_views` int DEFAULT 0 COMMENT '独立浏览量' AFTER `views`;
-- ALTER TABLE `article` ADD COLUMN `favorites` int DEFAULT 0 COMMENT '收藏数' AFTER `unique_views`;
-- ALTER TABLE `article` ADD COLUMN `tags` varchar(500) DEFAULT '' COMMENT '标签，JSON数组格式' AFTER `favorites`;
-- ALTER TABLE `article` ADD COLUMN `is_carousel` tinyint DEFAULT 0 COMMENT '是否轮播图文章，0否，1是' AFTER `tags`;

-- ============================================
-- 用户角色相关表结构
-- ============================================

-- 修改 user 表，添加 role 字段
ALTER TABLE `user` ADD COLUMN `role` varchar(50) DEFAULT 'ROLE_USER' COMMENT '角色';

-- 新建 role 表
CREATE TABLE IF NOT EXISTS `role` (
  `id` int NOT NULL AUTO_INCREMENT,
  `role_name` varchar(50) NOT NULL COMMENT '角色名称',
  `role_alias` varchar(50) NOT NULL COMMENT '角色标识',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 新建 user_role 表
CREATE TABLE IF NOT EXISTS `user_role` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL COMMENT '用户ID',
  `role_id` int NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 初始化角色数据
INSERT INTO `role` (`role_name`, `role_alias`) VALUES
  ('普通用户', 'ROLE_USER'),
  ('内容管理员', 'ROLE_CONTENT_ADMIN'),
  ('超级管理员', 'ROLE_SUPER_ADMIN');

-- ============================================
-- 推送任务表（推送平台 demo 模块）
-- 状态机: PENDING -> SENDING -> SUCCESS / FAILED -> SENDING | DEAD -> SUCCESS
-- ============================================
CREATE TABLE IF NOT EXISTS `push_task` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `message_id` VARCHAR(64) NOT NULL COMMENT '消息唯一ID（幂等key）',
  `user_id` VARCHAR(64) NOT NULL COMMENT '接收用户ID',
  `content` VARCHAR(500) NOT NULL COMMENT '推送内容',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SENDING/SUCCESS/FAILED/DEAD',
  `retry_count` INT NOT NULL DEFAULT 0 COMMENT '已重试次数',
  `last_error` VARCHAR(500) DEFAULT NULL COMMENT '最近一次错误信息',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_message_id` (`message_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息推送任务表';


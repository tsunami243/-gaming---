-- Dedicated non-human owner for content accepted from the authenticated ingest pipeline.
INSERT INTO `user` (username, password, nickname, role)
VALUES ('ingest-system', '!', '采集系统', 'ROLE_CONTENT_ADMIN') AS seeded
ON DUPLICATE KEY UPDATE
    nickname = seeded.nickname,
    role = seeded.role;

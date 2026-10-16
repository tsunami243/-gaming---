-- The only enabled games for the first release.
INSERT INTO game (code, name_zh, name_original, aliases, enabled) VALUES
    ('delta-force', '三角洲行动', 'Delta Force', JSON_ARRAY('三角洲行动', 'Delta Force'), 1),
    ('valorant', '无畏契约', 'VALORANT', JSON_ARRAY('无畏契约', 'VALORANT', 'Valorant'), 1),
    ('cs2', 'Counter-Strike 2', 'Counter-Strike 2', JSON_ARRAY('CS2', 'Counter-Strike 2', '反恐精英2'), 1),
    ('apex-legends', 'Apex Legends', 'Apex Legends', JSON_ARRAY('Apex Legends', 'Apex'), 1);

-- Preserve legacy category IDs expected by the current Vue/Python code during the API migration.
INSERT INTO category (id, category_name, category_alias, create_user) VALUES
    (5, '三角洲行动', 'delta-force', NULL),
    (8, '无畏契约', 'valorant', NULL),
    (9, 'Counter-Strike', 'cs2', NULL),
    (10, 'Apex Legends', 'apex-legends', NULL) AS seeded
ON DUPLICATE KEY UPDATE
    category_alias = seeded.category_alias,
    update_time = CURRENT_TIMESTAMP;

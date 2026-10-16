-- Align the Delta Force source identity with its real first-party entry point.
-- The source code stays `delta-force-garena-official` on purpose: the V8 seed, the source
-- policy gate and the migration tests all key on it, and renaming it would break the
-- documented publication gate. Only the displayed identity and origin metadata change.
UPDATE content_source
SET name = '三角洲行动官方公告（国服官网）',
    base_url = 'https://df.qq.com/cp/a20240906main/newslist.html',
    allowed_domains = JSON_ARRAY('df.qq.com', 'comm.ams.game.qq.com'),
    language = 'zh-CN'
WHERE code = 'delta-force-garena-official';

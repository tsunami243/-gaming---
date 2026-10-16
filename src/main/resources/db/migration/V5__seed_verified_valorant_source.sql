-- First verified source: Riot official VALORANT patch notes. Shadow only; never auto-publish.
INSERT INTO content_source (code,name,source_type,base_url,allowed_domains,game_scope,language,adapter_key,schedule_hours,candidate_limit,auto_publish_enabled,verification_status)
VALUES ('valorant-official','VALORANT 官方更新公告','OFFICIAL','https://playvalorant.com/en-us/news/game-updates/',JSON_ARRAY('playvalorant.com'),JSON_ARRAY('valorant'),'en','valorant-official',24,10,0,'SHADOW') AS seeded
ON DUPLICATE KEY UPDATE
    name=seeded.name, base_url=seeded.base_url, allowed_domains=seeded.allowed_domains, game_scope=seeded.game_scope,
    language=seeded.language, adapter_key=seeded.adapter_key, schedule_hours=seeded.schedule_hours,
    candidate_limit=seeded.candidate_limit, auto_publish_enabled=0,
    verification_status=IF(content_source.verification_status='PAUSED','PAUSED',seeded.verification_status);

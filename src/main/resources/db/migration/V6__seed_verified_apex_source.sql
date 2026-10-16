-- EA official Apex Legends news. Shadow only pending quality evaluation.
INSERT INTO content_source (code,name,source_type,base_url,allowed_domains,game_scope,language,adapter_key,schedule_hours,candidate_limit,auto_publish_enabled,verification_status)
VALUES ('apex-official','Apex Legends 官方公告（EA）','OFFICIAL','https://www.ea.com/games/apex-legends/news',JSON_ARRAY('ea.com'),JSON_ARRAY('apex-legends'),'en','apex-official',24,10,0,'SHADOW') AS seeded
ON DUPLICATE KEY UPDATE
 name=seeded.name,base_url=seeded.base_url,allowed_domains=seeded.allowed_domains,game_scope=seeded.game_scope,language=seeded.language,adapter_key=seeded.adapter_key,schedule_hours=seeded.schedule_hours,candidate_limit=seeded.candidate_limit,auto_publish_enabled=0,verification_status=IF(content_source.verification_status='PAUSED','PAUSED',seeded.verification_status);

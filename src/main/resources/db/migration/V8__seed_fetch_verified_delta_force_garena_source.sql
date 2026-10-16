-- Delta Force English official Garena regional news. Fetch-verified only; shadow/auto publish remain disabled.
INSERT INTO content_source (code,name,source_type,base_url,allowed_domains,game_scope,language,adapter_key,schedule_hours,candidate_limit,auto_publish_enabled,verification_status)
VALUES ('delta-force-garena-official','Delta Force 官方公告（Garena 区域）','OFFICIAL','https://deltaforce.garena.com/en/',JSON_ARRAY('deltaforce.garena.com'),JSON_ARRAY('delta-force'),'en','delta-force-garena-official',24,10,0,'FETCH_VERIFIED') AS seeded
ON DUPLICATE KEY UPDATE
 name=seeded.name,base_url=seeded.base_url,allowed_domains=seeded.allowed_domains,game_scope=seeded.game_scope,language=seeded.language,adapter_key=seeded.adapter_key,schedule_hours=seeded.schedule_hours,candidate_limit=seeded.candidate_limit,auto_publish_enabled=0,verification_status=IF(content_source.verification_status='PAUSED','PAUSED',seeded.verification_status);

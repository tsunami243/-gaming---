-- Official Apex Legends Global Series news. Fetch-verified only; never auto-publish without the quality gate.
INSERT INTO content_source (code,name,source_type,base_url,allowed_domains,game_scope,language,adapter_key,schedule_hours,candidate_limit,auto_publish_enabled,verification_status)
VALUES ('algs-official','Apex Legends Global Series 官方赛事资讯','OFFICIAL','https://algs.ea.com/en/news',JSON_ARRAY('algs.ea.com'),JSON_ARRAY('apex-legends'),'en','algs-official',12,10,0,'FETCH_VERIFIED') AS seeded
ON DUPLICATE KEY UPDATE
 name=seeded.name,base_url=seeded.base_url,allowed_domains=seeded.allowed_domains,game_scope=seeded.game_scope,language=seeded.language,adapter_key=seeded.adapter_key,schedule_hours=seeded.schedule_hours,candidate_limit=seeded.candidate_limit,auto_publish_enabled=0,verification_status=IF(content_source.verification_status='PAUSED','PAUSED',seeded.verification_status);

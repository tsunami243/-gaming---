-- Counter-Strike 2 official Steam News API. Fetch-verified only; automatic publication remains disabled.
INSERT INTO content_source (code,name,source_type,base_url,allowed_domains,game_scope,language,adapter_key,schedule_hours,candidate_limit,auto_publish_enabled,verification_status)
VALUES ('cs2-official','Counter-Strike 2 官方公告（Steam）','OFFICIAL','https://api.steampowered.com/ISteamNews/GetNewsForApp/v0002/?appid=730&count=10&maxlength=5000&format=json',JSON_ARRAY('api.steampowered.com'),JSON_ARRAY('cs2'),'en','cs2-official',24,10,0,'FETCH_VERIFIED') AS seeded
ON DUPLICATE KEY UPDATE
 name=seeded.name,base_url=seeded.base_url,allowed_domains=seeded.allowed_domains,game_scope=seeded.game_scope,language=seeded.language,adapter_key=seeded.adapter_key,schedule_hours=seeded.schedule_hours,candidate_limit=seeded.candidate_limit,auto_publish_enabled=0,verification_status=IF(content_source.verification_status='PAUSED','PAUSED',seeded.verification_status);

package org.example.bigevent.service.impl;

import org.example.bigevent.pojo.Article;
import org.example.bigevent.service.ArticleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Polls the Redis ingest queue every N seconds and creates draft articles.
 *
 * Queue contract (with Python side):
 *   Python RPUSH  →  ingest:article_queue
 *   Java    LPOP  ←  ingest:article_queue
 */
@Component
public class ArticleIngestConsumer {

    private static final Logger log = LoggerFactory.getLogger(ArticleIngestConsumer.class);
    private static final String QUEUE_KEY = "ingest:article_queue";
    private static final int POLL_INTERVAL_MS = 10_000;

    /** System user ID used as createUser for ingested articles. */
    private static final int SYSTEM_USER_ID = 1;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ArticleService articleService;

    private final JsonParser jsonParser = JsonParserFactory.getJsonParser();

    @PostConstruct
    public void init() {
        log.info("ArticleIngestConsumer started — queue: {}", QUEUE_KEY);
    }

    @Scheduled(initialDelay = 5_000, fixedDelay = POLL_INTERVAL_MS)
    public void consume() {
        String payload = stringRedisTemplate.opsForList().leftPop(QUEUE_KEY);
        if (payload == null) {
            return;
        }

        try {
            Map<String, Object> map = jsonParser.parseMap(payload);
            Article article = toArticle(map);
            articleService.addArticle(article, SYSTEM_USER_ID);

            log.info("Ingested: [{}] {}",
                    map.getOrDefault("source", ""),
                    String.valueOf(map.getOrDefault("title", "")).substring(0,
                            Math.min(40, String.valueOf(map.getOrDefault("title", "")).length())));

        } catch (Exception e) {
            stringRedisTemplate.opsForList().rightPush(QUEUE_KEY, payload);
            log.error("Ingest failed, pushed back to queue: {}", e.getMessage());
        }
    }

    private Article toArticle(Map<String, Object> map) {
        Article article = new Article();
        article.setTitle(str(map, "title"));
        article.setContent(str(map, "content"));
        article.setCategoryId(intVal(map, "category_id"));
        article.setCoverImg(str(map, "cover_img"));
        article.setTags(str(map, "tags"));
        article.setState("草稿");
        return article;
    }

    private static String str(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v == null ? "" : v.toString();
    }

    private static Integer intVal(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v instanceof Number n) return n.intValue();
        return 0;
    }
}

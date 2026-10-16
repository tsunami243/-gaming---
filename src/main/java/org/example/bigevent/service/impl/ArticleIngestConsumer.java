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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.Map;

/**
 * 采集队列消费者（legacy 通道）：定时轮询 Redis 列表并把消息落成草稿文章。
 *
 * <p>本类已被内部 HTTP 入库接口（{@code InternalIngestController}）取代，
 * 默认关闭，仅当配置 {@code ingest.legacy-queue.enabled=true} 时装配。</p>
 *
 * <p>队列契约（与 Python 采集侧约定）：</p>
 * <pre>
 *   Python 端 RPUSH  →  ingest:article_queue
 *   Java   端 LPOP   ←  ingest:article_queue
 * </pre>
 */
@Component
@ConditionalOnProperty(name = "ingest.legacy-queue.enabled", havingValue = "true")
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

    /** 启动时打印队列名，便于确认 legacy 通道确实被装配。 */
    @PostConstruct
    public void init() {
        log.info("ArticleIngestConsumer started — queue: {}", QUEUE_KEY);
    }

    /**
     * 轮询消费一条队列消息：解析 JSON → 转 Article → 落库为草稿。
     *
     * <p>采用「先弹出、失败再推回队尾」的策略，避免坏消息阻塞队列；
     * 单次只处理一条，失败时把原始载荷放回，等待下一轮重试。</p>
     */
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

    /**
     * 把队列消息映射为草稿文章实体。
     *
     * @param map 消息键值对，需包含 title 与 content
     * @return 状态为「草稿」的 Article
     * @throws IllegalArgumentException 标题为空或正文不足 50 字符时抛出（触发消息回队重试）
     */
    Article toArticle(Map<String, Object> map) {
        String title = str(map, "title");
        if (title.isBlank()) {
            throw new IllegalArgumentException("Article title must not be blank");
        }
        String content = str(map, "content");
        if (content.length() < 50) {
            throw new IllegalArgumentException("Article content too short: " + content.length() + " chars");
        }
        Article article = new Article();
        article.setTitle(title);
        article.setContent(content);
        article.setCategoryId(intVal(map, "category_id"));
        article.setCoverImg(str(map, "cover_img"));
        article.setTags(str(map, "tags"));
        article.setState("草稿");
        return article;
    }

    /**
     * 读取字符串字段，缺失时返回空串。
     *
     * @param map 消息键值对
     * @param key 字段名
     * @return 字段文本，缺失为 {@code ""}
     */
    private static String str(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v == null ? "" : v.toString();
    }

    /**
     * 读取整型字段，缺失或非数字时返回 0。
     *
     * @param map 消息键值对
     * @param key 字段名
     * @return 字段整数值，缺失为 {@code 0}
     */
    private static Integer intVal(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v instanceof Number n) return n.intValue();
        return 0;
    }
}


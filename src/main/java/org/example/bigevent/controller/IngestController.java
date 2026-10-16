package org.example.bigevent.controller;

import org.example.bigevent.pojo.Article;
import org.example.bigevent.pojo.Result;
import org.example.bigevent.service.ArticleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 旧采集队列手动消费（legacy，默认不注册）。
 *
 * <p>仅在配置 {@code ingest.legacy-queue.enabled=true} 时才会被装配（{@link ConditionalOnProperty}），
 * 默认关闭；新采集链路已改为内部 HTTP + 持久 outbox，本接口只用于一次性迁移旧 Redis 队列。
 * 请求需 CONTENT_ADMIN / SUPER_ADMIN（由 {@code /admin/**} 规则保护）。
 */
@RestController
@ConditionalOnProperty(name = "ingest.legacy-queue.enabled", havingValue = "true")
public class IngestController {

    private static final String QUEUE_KEY = "ingest:article_queue";
    private static final int SYSTEM_USER_ID = 1;

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private ArticleService articleService;

    /**
     * 从旧队列取出并消费一条记录（每次仅一条）。
     *
     * <p>{@code POST /admin/ingest/consume}
     *
     * @return 消费结果描述（含队列前后长度）；队列为空返回 "queue empty"，异常返回错误信息
     */
    @PostMapping("/admin/ingest/consume")
    public Result consume() {
        try {
            long before = redis.opsForList().size(QUEUE_KEY);
            String payload = redis.opsForList().leftPop(QUEUE_KEY);
            if (payload == null) {
                return Result.success("queue empty");
            }

            JsonParser parser = JsonParserFactory.getJsonParser();
            Map<String, Object> map = parser.parseMap(payload);

            Article article = new Article();
            String title = str(map, "title");
            if (title.length() > 200) title = title.substring(0, 200);
            Integer catId = intVal(map, "category_id");
            article.setTitle(title);
            article.setContent(str(map, "content"));
            article.setCategoryId(catId > 0 ? catId : null);
            article.setCoverImg(str(map, "cover_img"));
            article.setTags(str(map, "tags"));
            article.setState("草稿");

            articleService.addArticle(article, SYSTEM_USER_ID);

            long after = redis.opsForList().size(QUEUE_KEY);
            return Result.success("ingested: " + str(map, "title").substring(0, Math.min(30, str(map, "title").length())) + " (q: " + before + "→" + after + ")");

        } catch (Exception e) {
            return Result.error(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    /** 读取 Map 中的字符串字段，缺失返回空串。 */
    private static String str(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v == null ? "" : v.toString();
    }

    /** 读取 Map 中的整数字段，缺失或非数字返回 0。 */
    private static Integer intVal(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v instanceof Number n) return n.intValue();
        return 0;
    }
}

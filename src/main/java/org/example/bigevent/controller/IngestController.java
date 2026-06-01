package org.example.bigevent.controller;

import org.example.bigevent.pojo.Article;
import org.example.bigevent.pojo.Result;
import org.example.bigevent.service.ArticleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class IngestController {

    private static final String QUEUE_KEY = "ingest:article_queue";
    private static final int SYSTEM_USER_ID = 1;

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private ArticleService articleService;

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

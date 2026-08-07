package org.example.bigevent.service;

import org.example.bigevent.pojo.Article;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 热度服务：Redis ZSet 排行榜缓存层。
 * 事件增量与 MySQL 公式 views*1 + uniqueViews*3 + favorites*5 + likeCount*2 + commentCount*3 等价：
 * 普通浏览 +1、首次独立浏览 +4(1+3)、点赞 +2、收藏 +5、评论 +3。
 * 所有 Redis 操作均做降级：异常只记日志不抛出，调用方回退 MySQL。
 */
@Service
public class HeatService {

    private static final Logger log = LoggerFactory.getLogger(HeatService.class);

    /** 热度排行榜 ZSet key */
    public static final String RANK_KEY = "hot:rank";
    /** 独立浏览去重 Set 前缀：hot:view:{articleId} */
    private static final String VIEW_KEY_PREFIX = "hot:view:";
    /** 独立浏览窗口：24 小时（与 MySQL view_history 一致） */
    private static final long VIEW_WINDOW_SECONDS = 24 * 3600;

    public static final double DELTA_VIEW_REPEAT = 1;
    public static final double DELTA_VIEW_UNIQUE = 4;
    public static final double DELTA_LIKE = 2;
    public static final double DELTA_FAVORITE = 5;
    public static final double DELTA_COMMENT = 3;

    @Autowired
    private StringRedisTemplate redis;

    /**
     * 浏览事件：Redis Set 判重（SADD 成功=窗口内首次）→ ZSet 累加。
     * member 约定：登录用户 "u{userId}"，游客 "ip:{ip}"
     */
    public void recordView(Integer articleId, String member) {
        try {
            String key = VIEW_KEY_PREFIX + articleId;
            Long added = redis.opsForSet().add(key, member);
            if (added != null && added > 0) {
                redis.expire(key, VIEW_WINDOW_SECONDS, TimeUnit.SECONDS);
            }
            double delta = (added != null && added > 0) ? DELTA_VIEW_UNIQUE : DELTA_VIEW_REPEAT;
            incr(articleId, delta);
        } catch (Exception e) {
            log.warn("recordView heat failed, articleId={}: {}", articleId, e.getMessage());
        }
    }

    /** 点赞/收藏/评论等行为：按增量累加 */
    public void incr(Integer articleId, double delta) {
        try {
            redis.opsForZSet().incrementScore(RANK_KEY, String.valueOf(articleId), delta);
        } catch (Exception e) {
            log.warn("incr heat failed, articleId={}: {}", articleId, e.getMessage());
        }
    }

    /**
     * 热榜 TopN：score 降序，同分按文章 ID 升序。
     * Redis 异常或 ZSet 为空返回 null（调用方降级 MySQL）。
     */
    public List<Integer> topN(int n) {
        try {
            Set<TypedTuple<String>> tuples = redis.opsForZSet().reverseRangeWithScores(RANK_KEY, 0, -1);
            if (tuples == null || tuples.isEmpty()) {
                return null;
            }
            List<int[]> list = new ArrayList<>(); // [articleId, score]
            for (TypedTuple<String> t : tuples) {
                if (t.getValue() == null || t.getScore() == null) continue;
                list.add(new int[]{Integer.parseInt(t.getValue()), t.getScore().intValue()});
            }
            list.sort(Comparator.<int[]>comparingInt(a -> a[1]).reversed()
                    .thenComparingInt(a -> a[0]));
            List<Integer> ids = new ArrayList<>();
            for (int i = 0; i < list.size() && i < n; i++) {
                ids.add(list.get(i)[0]);
            }
            return ids;
        } catch (Exception e) {
            log.warn("topN failed: {}", e.getMessage());
            return null;
        }
    }

    /** 单篇热度：ZSet score，缺失返回 null（调用方回退 MySQL 公式） */
    public Integer score(Integer articleId) {
        try {
            Double s = redis.opsForZSet().score(RANK_KEY, String.valueOf(articleId));
            return s == null ? null : s.intValue();
        } catch (Exception e) {
            log.warn("score failed, articleId={}: {}", articleId, e.getMessage());
            return null;
        }
    }

    /** ZSet 是否为空（用于启动重建判断）；Redis 不可用视为空 */
    public boolean isEmpty() {
        try {
            Long size = redis.opsForZSet().zCard(RANK_KEY);
            return size == null || size == 0;
        } catch (Exception e) {
            log.warn("isEmpty failed: {}", e.getMessage());
            return true;
        }
    }

    /** 启动重建：从 MySQL 全量热度写入 ZSet（仅当 ZSet 为空时调用） */
    public void rebuild(List<Article> articleHeats) {
        try {
            if (articleHeats == null || articleHeats.isEmpty()) {
                return;
            }
            for (Article a : articleHeats) {
                if (a.getId() != null && a.getHeat() != null) {
                    redis.opsForZSet().add(RANK_KEY, String.valueOf(a.getId()), a.getHeat());
                }
            }
            log.info("rebuild hot:rank from DB, size={}", articleHeats.size());
        } catch (Exception e) {
            log.warn("rebuild failed: {}", e.getMessage());
        }
    }
}
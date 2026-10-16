package org.example.bigevent.service.impl;

import org.example.bigevent.pojo.Article;
import org.example.bigevent.service.HeatService;
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
 * 热度榜实现：以 Redis ZSet（{@code hot:rank}）承载文章热度分数。
 *
 * <p>设计上行全链路容灾——所有读写都包在 try/catch 内，Redis 故障时只告警不抛出，
 * 保证热度这样的「锦上添花」能力不会拖垮主流程（文章浏览/评论仍可正常返回）。
 * 榜单数据可由 {@link #rebuild(List)} 从 MySQL 重建。</p>
 */
@Service
public class HeatServiceImpl implements HeatService {

    private static final Logger log = LoggerFactory.getLogger(HeatServiceImpl.class);

    /** 独立浏览去重 Set 前缀：hot:view:{articleId} */
    private static final String VIEW_KEY_PREFIX = "hot:view:";
    /** 独立浏览窗口：24 小时（与 MySQL view_history 一致） */
    private static final long VIEW_WINDOW_SECONDS = 24 * 3600;

    @Autowired
    private StringRedisTemplate redis;

    /**
     * 记录一次浏览：同一 {@code member}（用户或 IP）24 小时内只计一次「独立浏览」。
     *
     * <p>首次浏览加 {@link HeatService#DELTA_VIEW_UNIQUE}，重复浏览加
     * {@link HeatService#DELTA_VIEW_REPEAT}（较小权重）。</p>
     *
     * @param articleId 文章 ID
     * @param member    去重标识（用户 ID 或访客 IP）
     */
    @Override
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

    /**
     * 给指定文章的分数累加 {@code delta}（评论、点赞等行为的热度埋点入口）。
     *
     * @param articleId 文章 ID
     * @param delta     增量，可为负
     */
    @Override
    public void incr(Integer articleId, double delta) {
        try {
            redis.opsForZSet().incrementScore(RANK_KEY, String.valueOf(articleId), delta);
        } catch (Exception e) {
            log.warn("incr heat failed, articleId={}: {}", articleId, e.getMessage());
        }
    }

    /**
     * 取热度榜前 {@code n} 篇文章 ID，按分数降序；分数相同时按 ID 升序保证顺序稳定。
     *
     * @param n 期望返回的条数
     * @return 文章 ID 列表；榜单为空或 Redis 异常时返回 {@code null}（调用方据此降级回库查询）
     */
    @Override
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

    /**
     * 查询单篇文章的当前热度分。
     *
     * @param articleId 文章 ID
     * @return 热度分（取整）；不存在或 Redis 异常时返回 {@code null}
     */
    @Override
    public Integer score(Integer articleId) {
        try {
            Double s = redis.opsForZSet().score(RANK_KEY, String.valueOf(articleId));
            return s == null ? null : s.intValue();
        } catch (Exception e) {
            log.warn("score failed, articleId={}: {}", articleId, e.getMessage());
            return null;
        }
    }

    /**
     * 判断热度榜是否为空。
     *
     * @return 榜单为空或 Redis 不可用时返回 {@code true}（fail-safe：宁可当作空，触发上层重建）
     */
    @Override
    public boolean isEmpty() {
        try {
            Long size = redis.opsForZSet().zCard(RANK_KEY);
            return size == null || size == 0;
        } catch (Exception e) {
            log.warn("isEmpty failed: {}", e.getMessage());
            return true;
        }
    }

    /**
     * 用数据库中的热度值重建 Redis 热度榜（Redis 数据丢失后的恢复入口）。
     *
     * @param articleHeats 携带 {@code id} 与 {@code heat} 的文章列表，为空时直接返回
     */
    @Override
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

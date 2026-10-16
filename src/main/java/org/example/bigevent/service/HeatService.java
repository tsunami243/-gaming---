package org.example.bigevent.service;

import org.example.bigevent.pojo.Article;

import java.util.List;

/**
 * 热度服务：Redis ZSet 排行榜缓存层。
 * 事件增量与 MySQL 公式 views*1 + uniqueViews*3 + favorites*5 + likeCount*2 + commentCount*3 等价：
 * 普通浏览 +1、首次独立浏览 +4(1+3)、点赞 +2、收藏 +5、评论 +3。
 * 所有 Redis 操作均做降级：异常只记日志不抛出，调用方回退 MySQL。
 */
public interface HeatService {

    /** 热度排行榜 ZSet key */
    String RANK_KEY = "hot:rank";

    double DELTA_VIEW_REPEAT = 1;
    double DELTA_VIEW_UNIQUE = 4;
    double DELTA_LIKE = 2;
    double DELTA_FAVORITE = 5;
    double DELTA_COMMENT = 3;

    /** 浏览事件：Redis Set 判重（SADD 成功=窗口内首次）→ ZSet 累加。member 约定：登录用户 "u{userId}"，游客 "ip:{ip}" */
    void recordView(Integer articleId, String member);

    /** 点赞/收藏/评论等行为：按增量累加 */
    void incr(Integer articleId, double delta);

    /** 热榜 TopN：score 降序，同分按文章 ID 升序。Redis 异常或 ZSet 为空返回 null（调用方降级 MySQL）。 */
    List<Integer> topN(int n);

    /** 单篇热度：ZSet score，缺失返回 null（调用方回退 MySQL 公式） */
    Integer score(Integer articleId);

    /** ZSet 是否为空（用于启动重建判断）；Redis 不可用视为空 */
    boolean isEmpty();

    /** 启动重建：从 MySQL 全量热度写入 ZSet（仅当 ZSet 为空时调用） */
    void rebuild(List<Article> articleHeats);
}

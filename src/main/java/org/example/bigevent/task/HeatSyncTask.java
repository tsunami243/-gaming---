package org.example.bigevent.task;

import jakarta.annotation.PostConstruct;
import org.example.bigevent.mapper.ArticleMapper;
import org.example.bigevent.service.HeatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 定时任务：把 Redis ZSet 热度回写 article.heat（默认每 5 分钟）。
 * 启动时若 ZSet 为空，从 MySQL 全量重建（防 Redis 数据丢失）。
 */
@Component
public class HeatSyncTask {

    private static final Logger log = LoggerFactory.getLogger(HeatSyncTask.class);

    @Autowired
    private StringRedisTemplate redis;
    @Autowired
    private ArticleMapper articleMapper;
    @Autowired
    private HeatService heatService;

    @PostConstruct
    public void init() {
        try {
            if (heatService.isEmpty()) {
                heatService.rebuild(articleMapper.selectAllHeat());
            }
        } catch (Exception e) {
            log.warn("init rebuild skipped: {}", e.getMessage());
        }
    }

    /** 每 5 分钟全量回写 ZSet score -> article.heat */
    @Scheduled(cron = "0 */5 * * * ?")
    public void syncHeat() {
        try {
            Set<TypedTuple<String>> tuples = redis.opsForZSet().rangeWithScores(HeatService.RANK_KEY, 0, -1);
            if (tuples == null) {
                return;
            }
            int updated = 0;
            for (TypedTuple<String> t : tuples) {
                if (t.getValue() == null || t.getScore() == null) {
                    continue;
                }
                try {
                    Integer id = Integer.parseInt(t.getValue());
                    articleMapper.updateHeat(id, t.getScore().intValue());
                    updated++;
                } catch (Exception e) {
                    log.warn("updateHeat failed, id={}: {}", t.getValue(), e.getMessage());
                }
            }
            log.info("HeatSyncTask done, updated={}", updated);
        } catch (Exception e) {
            log.error("HeatSyncTask error: {}", e.getMessage());
        }
    }
}
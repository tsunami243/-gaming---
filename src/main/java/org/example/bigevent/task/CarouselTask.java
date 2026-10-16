package org.example.bigevent.task;

import org.example.bigevent.mapper.ArticleMapper;
import org.example.bigevent.service.HeatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 每日定时任务（默认 06:00）：清空旧轮播 → 取 ZSet 热度 Top5 → 更新 is_carousel。
 * Redis 不可用时按 MySQL article.heat 降级取 Top5。
 */
@Component
public class CarouselTask {

    private static final Logger log = LoggerFactory.getLogger(CarouselTask.class);

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private HeatService heatService;

    /**
     * 重建轮播位：先清空全部轮播标记，再写入当前热度 Top5。
     *
     * <p>整段包在 try/catch 内——定时任务失败只落日志，不影响其它调度任务。</p>
     */
    @Scheduled(cron = "0 0 6 * * ?")
    public void refreshCarousel() {
        try {
            articleMapper.resetCarousel();
            List<Integer> ids = heatService.topN(5);
            if (ids == null || ids.isEmpty()) {
                ids = articleMapper.selectHotIdsFromDb(5);
            }
            if (ids != null && !ids.isEmpty()) {
                articleMapper.setCarousel(ids);
                log.info("CarouselTask refreshed, top5={}", ids);
            }
        } catch (Exception e) {
            log.error("CarouselTask error: {}", e.getMessage());
        }
    }
}

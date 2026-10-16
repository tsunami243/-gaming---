package org.example.bigevent.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 任务调度配置：启用 {@code @Scheduled} 并指定调度线程池。
 *
 * <p>池大小固定为 2，且注册了错误处理器——被调度任务抛出的异常不会静默消失，
 * 而是以 error 级别落日志，便于发现类似「Redis 不支持某命令」这类持续失败。</p>
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {

    private static final Logger log = LoggerFactory.getLogger(SchedulingConfig.class);

    /**
     * 创建调度线程池。
     *
     * @return 线程名以 {@code scheduled-} 开头、异常会落日志的调度器
     */
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("scheduled-");
        scheduler.setErrorHandler(t ->
                log.error("Scheduled task error: {}", t.getMessage(), t));
        return scheduler;
    }
}

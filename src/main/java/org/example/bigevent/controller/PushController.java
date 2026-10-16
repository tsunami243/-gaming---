package org.example.bigevent.controller;

import org.example.bigevent.mapper.PushTaskMapper;
import org.example.bigevent.pojo.PushTask;
import org.example.bigevent.service.impl.DeadLetterReprocessor;
import org.example.bigevent.service.impl.PushProducer;
import org.example.bigevent.utils.MockWechatPushClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 推送平台 demo 的 REST 入口（演示模块）。
 *
 * <p>基础路径 {@code /push}。<b>当前在 SecurityConfig 中被 {@code denyAll}</b>，
 * 生产不可访问；如需启用，须先纳入 JWT 鉴权与管理策略。
 * 保留代码仅为历史演示，对应限流、重试、死信等概念验证。
 */
@RestController
@RequestMapping("/push")
public class PushController {

    private final PushProducer producer;
    private final PushTaskMapper mapper;
    private final DeadLetterReprocessor deadLetterReprocessor;
    private final MockWechatPushClient wechatClient;

    /**
     * 构造控制器。
     *
     * @param producer            推送任务生产者
     * @param mapper              推送任务读写
     * @param deadLetterReprocessor 死信重处理器
     * @param wechatClient        推送通道（模拟微信）
     */
    public PushController(PushProducer producer,
                          PushTaskMapper mapper,
                          DeadLetterReprocessor deadLetterReprocessor,
                          MockWechatPushClient wechatClient) {
        this.producer = producer;
        this.mapper = mapper;
        this.deadLetterReprocessor = deadLetterReprocessor;
        this.wechatClient = wechatClient;
    }

    /** 创建推送任务的请求体。 */
    public record PushTaskRequest(String userId, String content) {}

    /**
     * 创建单条推送任务。
     *
     * <p>{@code POST /push/tasks}
     *
     * @param req 含 {@code userId} 与 {@code content}
     * @return 创建后的任务
     */
    @PostMapping("/tasks")
    public PushTask create(@RequestBody PushTaskRequest req) {
        return producer.create(req.userId(), req.content());
    }

    /**
     * 批量创建推送任务（模拟"低电量定时批量推送"场景）。
     *
     * <p>{@code POST /push/simulate/batch?count=}
     *
     * @param count 创建条数，默认 100
     * @return 实际创建条数
     */
    @PostMapping("/simulate/batch")
    public Map<String, Object> batch(@RequestParam(defaultValue = "100") int count) {
        int created = 0;
        for (int i = 0; i < count; i++) {
            producer.create("user_" + (i % 100), "电量不足提醒：您的校园卡余额已低于 10 元（模拟消息 #" + i + "）");
            created++;
        }
        return Map.of("created", created);
    }

    /**
     * 推送统计（各状态计数与成功率）。
     *
     * <p>{@code GET /push/tasks/stats}
     *
     * @return total/pending/sending/success/failed/dead 及 successRate
     */
    @GetMapping("/tasks/stats")
    public Map<String, Object> stats() {
        long total = mapper.countByStatus("PENDING") + mapper.countByStatus("SENDING")
                   + mapper.countByStatus("SUCCESS") + mapper.countByStatus("FAILED")
                   + mapper.countByStatus("DEAD");
        long success = mapper.countByStatus("SUCCESS");
        long dead = mapper.countByStatus("DEAD");
        double successRate = total == 0 ? 0 : (double) success / total * 100;
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("pending", mapper.countByStatus("PENDING"));
        stats.put("sending", mapper.countByStatus("SENDING"));
        stats.put("success", success);
        stats.put("failed", mapper.countByStatus("FAILED"));
        stats.put("dead", dead);
        stats.put("successRate", String.format("%.1f%%", successRate));
        return stats;
    }

    /**
     * 查看死信任务（等待人工处理）。
     *
     * <p>{@code GET /push/tasks/dead}
     *
     * @return 状态为 DEAD 的任务列表
     */
    @GetMapping("/tasks/dead")
    public List<PushTask> deadTasks() {
        return mapper.listByStatus("DEAD");
    }

    /**
     * 手动触发死信补偿。
     *
     * <p>{@code POST /push/deadletters/reprocess}
     *
     * @return 固定结果 {@code {status: done}}
     */
    @PostMapping("/deadletters/reprocess")
    public Map<String, Object> reprocess() {
        deadLetterReprocessor.reprocessNow();
        return Map.of("status", "done");
    }

    /**
     * 模拟微信 API 的调用指标。
     *
     * <p>{@code GET /push/mock/metrics}
     *
     * @return 调用次数、成功/限流/失败数与成功率
     */
    @GetMapping("/mock/metrics")
    public Map<String, Object> mockMetrics() {
        return Map.of(
            "calls", wechatClient.getCalls(),
            "successes", wechatClient.getSuccesses(),
            "rateLimited", wechatClient.getRateLimited(),
            "failures", wechatClient.getFailures(),
            "successRate", String.format("%.1f%%", wechatClient.successRate() * 100)
        );
    }
}

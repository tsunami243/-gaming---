package org.example.bigevent.push;

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
 * 推送平台 demo 的 REST 入口。
 * 说明：demo 模块暂未挂到 Spring Security 认证链（permitAll），
 * 生产环境应像项目其他接口一样置于 JWT 鉴权之后（见 SecurityConfig）。
 */
@RestController
@RequestMapping("/push")
public class PushController {

    private final PushProducer producer;
    private final PushTaskMapper mapper;
    private final DeadLetterReprocessor deadLetterReprocessor;
    private final MockWechatPushClient wechatClient;

    public PushController(PushProducer producer,
                          PushTaskMapper mapper,
                          DeadLetterReprocessor deadLetterReprocessor,
                          MockWechatPushClient wechatClient) {
        this.producer = producer;
        this.mapper = mapper;
        this.deadLetterReprocessor = deadLetterReprocessor;
        this.wechatClient = wechatClient;
    }

    public record PushTaskRequest(String userId, String content) {}

    /** 创建单条推送任务 */
    @PostMapping("/tasks")
    public PushTask create(@RequestBody PushTaskRequest req) {
        return producer.create(req.userId(), req.content());
    }

    /** 批量创建推送任务（模拟"低电量定时批量推送"场景） */
    @PostMapping("/simulate/batch")
    public Map<String, Object> batch(@RequestParam(defaultValue = "100") int count) {
        int created = 0;
        for (int i = 0; i < count; i++) {
            producer.create("user_" + (i % 100), "电量不足提醒：您的校园卡余额已低于 10 元（模拟消息 #" + i + "）");
            created++;
        }
        return Map.of("created", created);
    }

    /** 推送统计：对应简历"推送成功率 92% -> 99.5%"的度量口径 */
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

    /** 查看死信任务（对应"补偿表"，等待人工处理） */
    @GetMapping("/tasks/dead")
    public List<PushTask> deadTasks() {
        return mapper.listByStatus("DEAD");
    }

    /** 手动触发死信补偿（对应"人工补偿"） */
    @PostMapping("/deadletters/reprocess")
    public Map<String, Object> reprocess() {
        deadLetterReprocessor.reprocessNow();
        return Map.of("status", "done");
    }

    /** 模拟微信 API 的调用指标 */
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


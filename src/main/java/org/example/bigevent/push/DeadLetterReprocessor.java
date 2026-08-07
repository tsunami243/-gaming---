package org.example.bigevent.push;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 死信消费者：周期性消费死信队列，尝试补偿；仍失败则保留 DEAD 状态等待人工处理。
 *
 * 对应简历"对死信每 30 分钟周期性消费，仍失败写入补偿表等待人工补偿"。
 * demo 里用 60 秒便于观察；生产可配置为 30 分钟。补偿结果落库（DEAD -> SUCCESS）。
 */
@Component
public class DeadLetterReprocessor {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterReprocessor.class);
    private static final String GROUP = "dead-consumers";

    private final StringRedisTemplate redis;
    private final PushTaskMapper mapper;
    private final PushTaskStateMachine stateMachine;
    private final MockWechatPushClient wechatClient;

    public DeadLetterReprocessor(StringRedisTemplate redis,
                                 PushTaskMapper mapper,
                                 PushTaskStateMachine stateMachine,
                                 MockWechatPushClient wechatClient) {
        this.redis = redis;
        this.mapper = mapper;
        this.stateMachine = stateMachine;
        this.wechatClient = wechatClient;
    }

    @Scheduled(initialDelay = 60_000, fixedDelay = 60_000)
    public void reprocess() {
        List<MapRecord<String, Object, Object>> records = redis.opsForStream().read(
            Consumer.from(GROUP, "dead-reprocessor"),
            StreamReadOptions.empty().count(50),
            StreamOffset.create(PushConsumer.DEAD_STREAM, ReadOffset.lastConsumed())
        );
        if (records == null) {
            return;
        }
        for (MapRecord<String, Object, Object> record : records) {
            String messageId = String.valueOf(record.getValue().get("messageId"));
            PushTask task = mapper.findByMessageId(messageId);
            if (task == null) {
                redis.opsForStream().acknowledge(PushConsumer.DEAD_STREAM, GROUP, record.getId());
                continue;
            }
            MockWechatPushClient.PushResult result = wechatClient.send(messageId, task.getUserId(), task.getContent());
            if (result.ok) {
                stateMachine.transition(task, PushTaskStatus.DEAD, PushTaskStatus.SUCCESS, null);
                log.info("dead letter compensated successfully: {}", messageId);
            } else {
                mapper.updateError(messageId, "死信补偿仍失败: " + result.error);
                log.warn("dead letter compensation failed, keep for manual handling: {}", messageId);
            }
            redis.opsForStream().acknowledge(PushConsumer.DEAD_STREAM, GROUP, record.getId());
        }
    }

    /** 手动触发补偿（对应"人工补偿"操作） */
    public void reprocessNow() {
        reprocess();
    }
}


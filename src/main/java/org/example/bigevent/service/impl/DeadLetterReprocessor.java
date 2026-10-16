package org.example.bigevent.service.impl;

import org.example.bigevent.mapper.PushTaskMapper;
import org.example.bigevent.pojo.PushTask;
import org.example.bigevent.pojo.PushTaskStatus;
import org.example.bigevent.utils.MockWechatPushClient;

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
 * 死信消费者：周期性消费死信队列并尝试补偿；仍失败则保留 {@code DEAD} 状态等待人工处理。
 *
 * <p>对应「对死信每 30 分钟周期性消费，仍失败写入补偿表等待人工补偿」。
 * demo 里用 60 秒便于观察；生产可配置为 30 分钟。补偿成功会落库（{@code DEAD -> SUCCESS}）。
 *
 * <p>无论补偿成功与否，读取到的消息都会被 ACK，避免同一条死信被反复消费；
 * 需要人工介入的任务靠数据库里的 {@code DEAD} 状态保留痕迹。
 */
@Component
public class DeadLetterReprocessor {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterReprocessor.class);

    /** 死信消费者的消费组名。 */
    private static final String GROUP = "dead-consumers";

    /** 单轮最多补偿的条数。 */
    private static final int BATCH_SIZE = 50;

    private final StringRedisTemplate redis;
    private final PushTaskMapper mapper;
    private final PushTaskStateMachine stateMachine;
    private final MockWechatPushClient wechatClient;

    /**
     * 构造死信重处理器。
     *
     * @param redis        Redis 客户端（读取死信流、操作幂等 key）
     * @param mapper       推送任务读写
     * @param stateMachine 推送任务状态机
     * @param wechatClient 推送通道（模拟微信）
     */
    public DeadLetterReprocessor(StringRedisTemplate redis,
                                 PushTaskMapper mapper,
                                 PushTaskStateMachine stateMachine,
                                 MockWechatPushClient wechatClient) {
        this.redis = redis;
        this.mapper = mapper;
        this.stateMachine = stateMachine;
        this.wechatClient = wechatClient;
    }

    /**
     * 单轮死信补偿：读取死信队列、逐条重试推送，并把结果回写状态。
     *
     * <p>任务已不存在时直接 ACK 丢弃；重试成功转为 {@code SUCCESS}，
     * 仍失败则记下错误并保持 {@code DEAD}，交由人工补偿。
     */
    @Scheduled(initialDelay = 60_000, fixedDelay = 60_000)
    public void reprocess() {
        List<MapRecord<String, Object, Object>> records = redis.opsForStream().read(
                Consumer.from(GROUP, "dead-reprocessor"),
                StreamReadOptions.empty().count(BATCH_SIZE),
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

    /**
     * 手动触发一轮补偿（对应「人工补偿」操作）。
     */
    public void reprocessNow() {
        reprocess();
    }
}

package org.example.bigevent.service.impl;

import org.example.bigevent.mapper.PushTaskMapper;
import org.example.bigevent.pojo.PushTask;
import org.example.bigevent.pojo.PushTaskStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * 推送生产者：先落库（状态 {@code PENDING}），再投递到 Redis Stream 消息队列。
 *
 * <p>对应「业务方仅发领域事件即可触达用户」：业务侧只调 {@link #create(String, String)}，
 * 不关心下游如何推、推给谁，解耦由队列完成。这里用 Redis Stream 模拟生产环境的
 * Kafka/RabbitMQ（同样的队列语义：消费者组、未确认消息）。
 *
 * <p>注意：Redis Streams 需要 Redis 5.0 及以上，低版本会在投递时报
 * {@code ERR unknown command}。
 */
@Component
public class PushProducer {

    private static final Logger log = LoggerFactory.getLogger(PushProducer.class);

    /** 推送任务队列的 Stream 名称。 */
    public static final String STREAM = "push:stream";

    private final StringRedisTemplate redis;
    private final PushTaskMapper mapper;

    /**
     * 构造推送生产者。
     *
     * @param redis  Redis 客户端（写入推送流与任务缓存）
     * @param mapper 推送任务读写
     */
    public PushProducer(StringRedisTemplate redis, PushTaskMapper mapper) {
        this.redis = redis;
        this.mapper = mapper;
    }

    /**
     * 创建一条推送任务并投递进队列。
     *
     * @param userId  接收者标识
     * @param content 推送内容
     * @return 已落库的任务（含生成的 messageId 与初始状态）
     */
    public PushTask create(String userId, String content) {
        String messageId = UUID.randomUUID().toString().replace("-", "");
        PushTask task = new PushTask();
        task.setMessageId(messageId);
        task.setUserId(userId);
        task.setContent(content);
        task.setStatus(PushTaskStatus.PENDING.name());
        task.setRetryCount(0);
        mapper.insert(task);

        // 投递消息（Kafka 等价物：producer.send；RabbitMQ 等价物：channel.basicPublish）
        redis.opsForStream().add(STREAM, Map.of(
                "messageId", messageId,
                "userId", userId,
                "content", content
        ));
        log.info("push task created and enqueued: {}", messageId);
        return task;
    }
}

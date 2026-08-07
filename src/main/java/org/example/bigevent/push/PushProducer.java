package org.example.bigevent.push;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * 推送生产者：落库（状态 PENDING）+ 投递到 Redis Stream 消息队列。
 *
 * 对应简历"业务方仅发领域事件即可触达学生"：
 * 业务侧只调 create()，不关心下游如何推、推给谁，解耦由队列完成。
 * 这里用 Redis Stream 模拟生产环境的 Kafka/RabbitMQ（同样的队列语义：消费者组、未确认消息）。
 */
@Component
public class PushProducer {

    private static final Logger log = LoggerFactory.getLogger(PushProducer.class);
    public static final String STREAM = "push:stream";

    private final StringRedisTemplate redis;
    private final PushTaskMapper mapper;

    public PushProducer(StringRedisTemplate redis, PushTaskMapper mapper) {
        this.redis = redis;
        this.mapper = mapper;
    }

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


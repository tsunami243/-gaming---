package org.example.bigevent.service.impl;
import org.example.bigevent.mapper.PushTaskMapper;
import org.example.bigevent.pojo.PushTask;
import org.example.bigevent.pojo.PushTaskStatus;
import org.example.bigevent.utils.MockWechatPushClient;
import org.example.bigevent.utils.TokenBucketRateLimiter;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 推送消费者：从 Redis Stream 消费组拉取消息，执行"幂等 -> 状态机 -> 限流 -> 推送 -> 重试/死信"完整链路。
 *
 * 对应简历：
 * - 多分区并行消费 -> 消费组 + 每 1 秒轮询批量拉取（生产环境多个消费者实例并行）
 * - 消费侧幂等 key + Redis SETNX -> 见 idempotencyKey 逻辑
 * - 令牌桶限流 -> 见 TokenBucketRateLimiter
 * - 失败重试死信队列 -> 见 retry/dead letter 逻辑
 */
@Component
public class PushConsumer {

    private static final Logger log = LoggerFactory.getLogger(PushConsumer.class);
    private static final String GROUP = "push-consumers";
    private static final String DEDUP_PREFIX = "push:dedup:";
    public static final String DEAD_STREAM = "push:deadletter";
    private static final Duration DEDUP_TTL = Duration.ofHours(24);

    private final StringRedisTemplate redis;
    private final RedisConnectionFactory connectionFactory;
    private final PushTaskMapper mapper;
    private final PushTaskStateMachine stateMachine;
    private final TokenBucketRateLimiter rateLimiter;
    private final MockWechatPushClient wechatClient;

    @Value("${push.token-bucket.capacity:50}")
    private int bucketCapacity;
    @Value("${push.token-bucket.refill-per-second:50}")
    private int bucketRefill;
    @Value("${push.max-retry:3}")
    private int maxRetry;

    private final String consumerName;

    /** 构造消费者：注入依赖，并以运行用户名生成消费者实例名（多实例部署时用于区分）。 */
    public PushConsumer(StringRedisTemplate redis,
                        RedisConnectionFactory connectionFactory,
                        PushTaskMapper mapper,
                        PushTaskStateMachine stateMachine,
                        TokenBucketRateLimiter rateLimiter,
                        MockWechatPushClient wechatClient) {
        this.redis = redis;
        this.connectionFactory = connectionFactory;
        this.mapper = mapper;
        this.stateMachine = stateMachine;
        this.rateLimiter = rateLimiter;
        this.wechatClient = wechatClient;
        this.consumerName = "consumer-" + System.getProperty("user.name", "local");
    }

    /** 启动时为正常流与死信流各建一次消费组（幂等），并打印当前消费者名。 */
    @PostConstruct
    public void init() {
        createGroupIfAbsent(PushProducer.STREAM, GROUP);
        createGroupIfAbsent(DEAD_STREAM, "dead-consumers");
        log.info("PushConsumer started: group={}, consumer={}", GROUP, consumerName);
    }

    /**
     * XGROUP CREATE key group 0 MKSTREAM：新组从流头开始消费历史消息；组已存在则忽略（幂等）。
     *
     * @param stream 流 key
     * @param group  消费组名
     */
    private void createGroupIfAbsent(String stream, String group) {
        try {
            redis.execute((RedisConnection conn) -> {
                conn.streamCommands().xGroupCreate(
                    stream.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    group,
                    ReadOffset.from("0"),
                    true
                );
                return null;
            });
        } catch (Exception e) {
            // BUSYGROUP 等：组已存在，属正常（多实例部署）
            log.debug("consumer group already exists or create failed: {} {}", stream, group, e);
        }
    }

    /**
     * 每秒轮询一次消费组，批量拉取最多 10 条消息逐条处理。
     *
     * <p>单条消息失败不会拖垮整批：记录日志后把消息重新投回队尾，等待下一轮续跑；
     * 无论成败都 ACK 掉当前记录，避免未确认消息堆积在 pending 列表。</p>
     */
    @Scheduled(initialDelay = 10_000, fixedDelay = 1_000)
    public void poll() {
        List<MapRecord<String, Object, Object>> records = redis.opsForStream().read(
            Consumer.from(GROUP, consumerName),
            StreamReadOptions.empty().count(10),
            StreamOffset.create(PushProducer.STREAM, ReadOffset.lastConsumed())
        );
        if (records == null) {
            return;
        }
        for (MapRecord<String, Object, Object> record : records) {
            try {
                handle(record);
            } catch (Exception e) {
                // 单条消息处理失败不能拖垮整批：记录日志并放回队列尾，由下一次消费续跑
                log.error("handle push message failed, requeue: {}", record.getId(), e);
                redis.opsForStream().add(PushProducer.STREAM, record.getValue());
            } finally {
                redis.opsForStream().acknowledge(PushProducer.STREAM, GROUP, record.getId());
            }
        }
    }

    /**
     * 处理单条推送消息：幂等判断 → 令牌桶限流 → 状态机流转 → 调推送接口 → 成功/重试/死信。
     *
     * <p>任一环节提前返回时都会删除幂等 key，保证消息可被重新处理（避免"锁住但没做"）。</p>
     *
     * @param record Redis Stream 消息记录
     */
    @SuppressWarnings("unchecked")
    private void handle(MapRecord<String, Object, Object> record) {
        Map<Object, Object> body = record.getValue();
        String messageId = String.valueOf(body.get("messageId"));
        String userId = String.valueOf(body.get("userId"));
        String content = String.valueOf(body.get("content"));

        // 1. 幂等：仅当任务已成功（DB 状态 SUCCESS）时跳过重复投递（Kafka 至少一次语义防护）
        //    幂等 key 存在但任务未成功 => 上次处理中途崩溃，删除 key 重新处理
        Boolean first = redis.opsForValue().setIfAbsent(DEDUP_PREFIX + messageId, "1", DEDUP_TTL);
        if (first == null || !first) {
            PushTask dup = mapper.findByMessageId(messageId);
            if (dup != null && PushTaskStatus.SUCCESS.name().equals(dup.getStatus())) {
                log.debug("duplicate delivery skipped (already success): {}", messageId);
                return;
            }
            redis.delete(DEDUP_PREFIX + messageId);
            redis.opsForValue().setIfAbsent(DEDUP_PREFIX + messageId, "1", DEDUP_TTL);
        }

        // 2. 令牌桶限流：拿不到令牌就放回队尾（背压），状态保持 PENDING
        if (!rateLimiter.tryAcquire("push:ratelimit:wechat", bucketCapacity, bucketRefill)) {
            redis.delete(DEDUP_PREFIX + messageId);
            redis.opsForStream().add(PushProducer.STREAM, body);
            return;
        }

        PushTask task = mapper.findByMessageId(messageId);
        if (task == null) {
            redis.delete(DEDUP_PREFIX + messageId);
            return;
        }

        // 3. 状态机：PENDING -> SENDING（乐观锁，防并发重复消费）
        PushTaskStatus current = PushTaskStatus.from(task.getStatus());
        if (current == PushTaskStatus.SUCCESS || current == PushTaskStatus.DEAD) {
            redis.delete(DEDUP_PREFIX + messageId);
            return;
        }
        if (current != PushTaskStatus.SENDING) {
            // 3b. 进入 SENDING（乐观锁防并发）；已是 SENDING 说明上次处理中途崩溃，直接续跑
            if (!stateMachine.transition(task, current, PushTaskStatus.SENDING, null)) {
                redis.delete(DEDUP_PREFIX + messageId);
                return;
            }
        }

        // 4. 调用（模拟）微信 API
        MockWechatPushClient.PushResult result = wechatClient.send(messageId, userId, content);

        if (result.ok) {
            stateMachine.transition(task, PushTaskStatus.SENDING, PushTaskStatus.SUCCESS, null);
            // 幂等 key 保留：后续重复消息直接跳过
            return;
        }

        mapper.bumpRetry(messageId);
        PushTask refreshed = mapper.findByMessageId(messageId);
        int retryCount = refreshed.getRetryCount() == null ? 1 : refreshed.getRetryCount();

        if (result.permanent || retryCount > maxRetry) {
            // 5a. 不可重试（用户退订）或重试耗尽 -> 死信队列
            stateMachine.transition(refreshed, PushTaskStatus.SENDING, PushTaskStatus.DEAD, result.error);
            redis.opsForStream().add(DEAD_STREAM, Map.of("messageId", messageId, "error", String.valueOf(result.error)));
            redis.delete(DEDUP_PREFIX + messageId);
            return;
        }

        // 5b. 可重试：FAILED 状态 + 删除幂等 key + 放回队尾
        stateMachine.transition(refreshed, PushTaskStatus.SENDING, PushTaskStatus.FAILED, result.error);
        redis.delete(DEDUP_PREFIX + messageId);
        redis.opsForStream().add(PushProducer.STREAM, body);
    }
}

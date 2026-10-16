package org.example.bigevent.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 分布式令牌桶限流器（Redis + Lua 原子实现）。
 *
 * 对应简历"令牌桶限流 + 规避微信 API 频控"：
 * - 令牌桶允许一定突发（区别于漏桶的恒定速率），适合批量推送场景；
 * - Lua 脚本保证"取令牌"操作在分布式环境下原子，多个消费者并发也不会超发；
 * - 限流值取下游安全水位的 50%（真实项目：微信 100 QPS 上限，我们压到 50）。
 */
@Component
public class TokenBucketRateLimiter {

    /**
     * 桶状态的 Redis TTL（秒）。
     *
     * <p>这是一个权衡点：TTL 到期后桶会以"满桶"重建，于是实际允许速率变成
     * {@code 稳态速率 + 容量/TTL}——TTL 太短（原先 10s）会让限流被周期性突破；
     * 拉长则这些 key 在 Redis 里驻留更久。60s 把周期性突破压到原先的 1/6，
     * 同时保证 key 仍会自行回收（Redis 侧是 noeviction，不能只靠淘汰兜底）。</p>
     */
    private static final int BUCKET_KEY_TTL_SECONDS = 60;

    private static final DefaultRedisScript<Long> SCRIPT = new DefaultRedisScript<>(
        // KEYS[1]=bucket key
        // ARGV[1]=容量, ARGV[2]=每秒补充速率, ARGV[3]=本次请求令牌数, ARGV[4]=当前秒, ARGV[5]=TTL 秒
        "local key = KEYS[1]\n" +
        "local capacity = tonumber(ARGV[1])\n" +
        "local refill = tonumber(ARGV[2])\n" +
        "local requested = tonumber(ARGV[3])\n" +
        "local now = tonumber(ARGV[4])\n" +
        "local ttl = tonumber(ARGV[5])\n" +
        "local tokens = tonumber(redis.call('GET', key) or capacity)\n" +
        "local last = tonumber(redis.call('GET', key .. ':ts') or now)\n" +
        // 多实例时钟偏斜或 NTP 回拨会让 now-last 为负；不夹紧就会把令牌扣成负数、把桶长期锁死
        "local elapsed = math.max(0, now - last)\n" +
        "tokens = math.min(capacity, tokens + elapsed * refill)\n" +
        "if tokens >= requested then\n" +
        "  redis.call('SET', key, tokens - requested, 'EX', ttl)\n" +
        "  redis.call('SET', key .. ':ts', now, 'EX', ttl)\n" +
        "  return 1\n" +
        "else\n" +
        "  redis.call('SET', key, tokens, 'EX', ttl)\n" +
        "  redis.call('SET', key .. ':ts', now, 'EX', ttl)\n" +
        "  return 0\n" +
        "end",
        Long.class
    );

    private final StringRedisTemplate redis;

    /**
     * 构造限流器。
     *
     * @param redis Redis 客户端，桶状态（令牌数与上次补充时间）存在 Redis 中
     */
    public TokenBucketRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * 尝试从桶中取走 1 个令牌。
     *
     * @param bucketKey       桶的 Redis key
     * @param capacity        桶容量（允许的突发上限）
     * @param refillPerSecond 每秒补充的令牌数
     * @return true 表示拿到了令牌，可以继续
     */
    public boolean tryAcquire(String bucketKey, int capacity, int refillPerSecond) {
        Long result = redis.execute(
            SCRIPT,
            List.of(bucketKey),
            String.valueOf(capacity),
            String.valueOf(refillPerSecond),
            "1",
            String.valueOf(System.currentTimeMillis() / 1000),
            String.valueOf(BUCKET_KEY_TTL_SECONDS)
        );
        return result != null && result == 1L;
    }
}


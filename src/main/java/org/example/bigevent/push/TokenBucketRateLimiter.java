package org.example.bigevent.push;

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

    private static final DefaultRedisScript<Long> SCRIPT = new DefaultRedisScript<>(
        // KEYS[1]=bucket key, ARGV[1]=容量, ARGV[2]=每秒补充速率, ARGV[3]=本次请求令牌数, ARGV[4]=当前秒
        "local key = KEYS[1]\n" +
        "local capacity = tonumber(ARGV[1])\n" +
        "local refill = tonumber(ARGV[2])\n" +
        "local requested = tonumber(ARGV[3])\n" +
        "local now = tonumber(ARGV[4])\n" +
        "local tokens = tonumber(redis.call('GET', key) or capacity)\n" +
        "local last = tonumber(redis.call('GET', key .. ':ts') or now)\n" +
        "tokens = math.min(capacity, tokens + (now - last) * refill)\n" +
        "if tokens >= requested then\n" +
        "  redis.call('SET', key, tokens - requested, 'EX', 10)\n" +
        "  redis.call('SET', key .. ':ts', now, 'EX', 10)\n" +
        "  return 1\n" +
        "else\n" +
        "  redis.call('SET', key, tokens, 'EX', 10)\n" +
        "  redis.call('SET', key .. ':ts', now, 'EX', 10)\n" +
        "  return 0\n" +
        "end",
        Long.class
    );

    private final StringRedisTemplate redis;

    public TokenBucketRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** @return true 表示拿到了令牌，可以继续 */
    public boolean tryAcquire(String bucketKey, int capacity, int refillPerSecond) {
        Long result = redis.execute(
            SCRIPT,
            List.of(bucketKey),
            String.valueOf(capacity),
            String.valueOf(refillPerSecond),
            "1",
            String.valueOf(System.currentTimeMillis() / 1000)
        );
        return result != null && result == 1L;
    }
}


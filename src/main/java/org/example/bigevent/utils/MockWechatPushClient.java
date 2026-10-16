package org.example.bigevent.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 模拟微信订阅消息 API（真实项目里这是外部 HTTP 调用）。
 *
 * 模拟行为（对应真实约束）：
 * - 频控：每秒最多 apiLimit QPS，超限返回 45009（真实微信错误码）；
 * - 网络失败：按 failureRate 概率随机失败（模拟网络波动）；
 * - 永久拒绝：用户取消订阅（permanentRejectRate），这类错误重试无用，直接进死信。
 *
 * 同时记录调用指标，供 /push/mock/metrics 查看——对应简历"成功率 92%→99.5%"的测量方法。
 */
@Component
public class MockWechatPushClient {

    private static final Logger log = LoggerFactory.getLogger(MockWechatPushClient.class);

    private final int apiLimit;
    private final double failureRate;
    private final double permanentRejectRate;

    private final AtomicInteger calls = new AtomicInteger();
    private final AtomicInteger successes = new AtomicInteger();
    private final AtomicInteger rateLimited = new AtomicInteger();
    private final AtomicInteger failures = new AtomicInteger();

    private final Object windowLock = new Object();
    private long windowStartSecond;
    private int windowCount;

    /**
     * 构造模拟客户端。
     *
     * @param apiLimit            每秒频控上限（超限返回微信 45009 错误码）
     * @param failureRate         模拟网络波动导致的可重试失败概率
     * @param permanentRejectRate 模拟用户取消订阅导致的不可重试失败概率
     */
    public MockWechatPushClient(
            @Value("${push.mock.api-limit:50}") int apiLimit,
            @Value("${push.mock.failure-rate:0.08}") double failureRate,
            @Value("${push.mock.permanent-reject-rate:0.01}") double permanentRejectRate) {
        this.apiLimit = apiLimit;
        this.failureRate = failureRate;
        this.permanentRejectRate = permanentRejectRate;
        this.windowStartSecond = System.currentTimeMillis() / 1000;
    }

    /**
     * 模拟发送一条订阅消息。
     *
     * <p>判定顺序：先过频控，再按随机数决定「永久拒绝」或「网络失败」，否则视为成功。</p>
     *
     * @param messageId 消息 ID（幂等键，模拟端仅用于日志语义）
     * @param userId    接收用户 ID
     * @param content   推送内容
     * @return 推送结果，区分成功 / 可重试失败 / 不可重试失败 / 被限流
     */
    public PushResult send(String messageId, String userId, String content) {
        if (!acquireQuota()) {
            rateLimited.incrementAndGet();
            return PushResult.rateLimited("45009 请求速度超过频控限制");
        }
        calls.incrementAndGet();

        double r = ThreadLocalRandom.current().nextDouble();
        if (r < permanentRejectRate) {
            failures.incrementAndGet();
            return PushResult.permanent("用户已取消订阅，重试无效");
        }
        if (r < permanentRejectRate + failureRate) {
            failures.incrementAndGet();
            return PushResult.network("模拟网络波动，推送失败");
        }
        successes.incrementAndGet();
        return PushResult.ok();
    }

    /**
     * 按「每秒滑动窗口」申请一次调用配额。
     *
     * <p>跨秒时重置计数器；加锁保证同一实例内计数不超发。</p>
     *
     * @return 拿到配额返回 {@code true}，本秒已超限返回 {@code false}
     */
    private boolean acquireQuota() {
        long nowSecond = System.currentTimeMillis() / 1000;
        synchronized (windowLock) {
            if (nowSecond != windowStartSecond) {
                windowStartSecond = nowSecond;
                windowCount = 0;
            }
            if (windowCount >= apiLimit) {
                return false;
            }
            windowCount++;
            return true;
        }
    }

    /** 成功率：对应简历"推送成功率"指标的测量口径 */
    public double successRate() {
        int total = calls.get();
        return total == 0 ? 1.0 : (double) successes.get() / total;
    }

    /**
     * 统计口径：实际发起调用的次数（不含被频控挡掉的请求）。
     *
     * @return 调用次数
     */
    public int getCalls() { return calls.get(); }

    /**
     * 统计口径：模拟推送成功的次数。
     *
     * @return 成功次数
     */
    public int getSuccesses() { return successes.get(); }

    /**
     * 统计口径：触发频控的次数。
     *
     * @return 被限流次数
     */
    public int getRateLimited() { return rateLimited.get(); }

    /**
     * 统计口径：调用失败的次数（含可重试与不可重试）。
     *
     * @return 失败次数
     */
    public int getFailures() { return failures.get(); }

    /**
     * 单次推送结果：区分成功、是否不可重试，以及错误说明。
     *
     * <p>消费者据此决定流转到 SUCCESS / FAILED（可重试）还是 DEAD（不可重试或重试耗尽）。</p>
     */
    public static class PushResult {
        /** 是否推送成功。 */
        public final boolean ok;
        /** 是否为不可重试失败（如用户退订），为真时直接进死信。 */
        public final boolean permanent;
        /** 错误说明；成功时为 {@code null}。 */
        public final String error;

        /**
         * 构造推送结果。
         *
         * @param ok        是否成功
         * @param permanent 是否不可重试
         * @param error     错误说明
         */
        private PushResult(boolean ok, boolean permanent, String error) {
            this.ok = ok;
            this.permanent = permanent;
            this.error = error;
        }

        /**
         * 构造成功结果。
         *
         * @return 成功结果
         */
        static PushResult ok() { return new PushResult(true, false, null); }

        /**
         * 构造被限流的结果（可重试）。
         *
         * @param msg 错误说明
         * @return 失败结果
         */
        static PushResult rateLimited(String msg) { return new PushResult(false, false, msg); }

        /**
         * 构造网络失败的结果（可重试）。
         *
         * @param msg 错误说明
         * @return 失败结果
         */
        static PushResult network(String msg) { return new PushResult(false, false, msg); }

        /**
         * 构造不可重试失败的结果。
         *
         * @param msg 错误说明
         * @return 失败结果，{@code permanent=true}
         */
        static PushResult permanent(String msg) { return new PushResult(false, true, msg); }
    }
}


package org.example.bigevent.push;

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

    public MockWechatPushClient(
            @Value("${push.mock.api-limit:50}") int apiLimit,
            @Value("${push.mock.failure-rate:0.08}") double failureRate,
            @Value("${push.mock.permanent-reject-rate:0.01}") double permanentRejectRate) {
        this.apiLimit = apiLimit;
        this.failureRate = failureRate;
        this.permanentRejectRate = permanentRejectRate;
        this.windowStartSecond = System.currentTimeMillis() / 1000;
    }

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

    public int getCalls() { return calls.get(); }
    public int getSuccesses() { return successes.get(); }
    public int getRateLimited() { return rateLimited.get(); }
    public int getFailures() { return failures.get(); }

    public static class PushResult {
        public final boolean ok;
        public final boolean permanent;
        public final String error;

        private PushResult(boolean ok, boolean permanent, String error) {
            this.ok = ok;
            this.permanent = permanent;
            this.error = error;
        }

        static PushResult ok() { return new PushResult(true, false, null); }
        static PushResult rateLimited(String msg) { return new PushResult(false, false, msg); }
        static PushResult network(String msg) { return new PushResult(false, false, msg); }
        static PushResult permanent(String msg) { return new PushResult(false, true, msg); }
    }
}


package org.example.bigevent.push;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 推送任务状态机（状态模式的核心：状态枚举 + 合法流转表）。
 *
 * 流转规则：
 *   PENDING  -> SENDING             消费者开始推送
 *   SENDING  -> SUCCESS             推送成功
 *   SENDING  -> FAILED              推送失败（可重试）
 *   SENDING  -> DEAD                单次发送即判定不可重试（用户退订）
 *   FAILED   -> SENDING             重试
 *   FAILED   -> DEAD                重试耗尽 / 不可重试（用户退订等）
 *   DEAD     -> SUCCESS             死信人工补偿成功
 */
public enum PushTaskStatus {
    PENDING, SENDING, SUCCESS, FAILED, DEAD;

    private static final Map<PushTaskStatus, Set<PushTaskStatus>> TRANSITIONS = Map.of(
        PENDING, EnumSet.of(SENDING),
        SENDING, EnumSet.of(SUCCESS, FAILED, DEAD),
        FAILED,  EnumSet.of(SENDING, DEAD),
        SUCCESS, EnumSet.noneOf(PushTaskStatus.class),
        DEAD,    EnumSet.of(SUCCESS)
    );

    public boolean canTransitionTo(PushTaskStatus target) {
        return TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }

    public static PushTaskStatus from(String value) {
        if (value == null || value.isBlank()) {
            return PENDING;
        }
        try {
            return PushTaskStatus.valueOf(value);
        } catch (IllegalArgumentException e) {
            return PENDING;
        }
    }
}

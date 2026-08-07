package org.example.bigevent.push;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 推送任务状态机（状态模式实现）。
 *
 * 设计要点（对应简历"工单状态机与事件驱动"）：
 * 1. 状态与流转规则收敛在 {@link PushTaskStatus}（单一数据源，杜绝 if-else 散落各处）；
 * 2. 每个状态可以注册进入时的副作用处理器（{@link StateHandler}），新增状态只需新增处理器；
 * 3. 落库用乐观锁（WHERE status = ?），防止并发下两个消费者同时推进同一任务。
 */
@Component
public class PushTaskStateMachine {

    private static final Logger log = LoggerFactory.getLogger(PushTaskStateMachine.class);

    /** 状态进入处理器：状态模式中"每个状态一个类"的轻量版，副作用都收在这里 */
    public interface StateHandler {
        void onEnter(PushTask task);
    }

    private final Map<PushTaskStatus, StateHandler> handlers = new EnumMap<>(PushTaskStatus.class);
    private final PushTaskMapper mapper;

    public PushTaskStateMachine(PushTaskMapper mapper) {
        this.mapper = mapper;
        // 注册各状态进入时的副作用（可以换成独立类，演示用 lambda 保持文件精简）
        handlers.put(PushTaskStatus.PENDING, task -> log.debug("task {} enters PENDING", task.getMessageId()));
        handlers.put(PushTaskStatus.SENDING, task -> log.debug("task {} enters SENDING", task.getMessageId()));
        handlers.put(PushTaskStatus.SUCCESS, task -> log.info("push success: {}", task.getMessageId()));
        handlers.put(PushTaskStatus.FAILED, task -> log.warn("push failed, will retry: {} err={}",
                task.getMessageId(), task.getLastError()));
        handlers.put(PushTaskStatus.DEAD, task -> log.error("task entered DEAD (needs manual compensation): {} err={}",
                task.getMessageId(), task.getLastError()));
    }

    /**
     * 校验并执行状态流转。非法流转直接抛异常；合法流转通过乐观锁落库。
     *
     * @return true 表示流转成功（行数=1）；false 表示并发下状态已被改变，本次流转无效
     */
    public boolean transition(PushTask task, PushTaskStatus from, PushTaskStatus to, String error) {
        if (!from.canTransitionTo(to)) {
            throw new IllegalStateException("非法状态流转: " + from + " -> " + to + " (task=" + task.getMessageId() + ")");
        }
        int rows = mapper.transition(task.getMessageId(), from.name(), to.name(), error);
        if (rows == 1) {
            task.setStatus(to.name());
            task.setLastError(error);
            handlers.getOrDefault(to, t -> { }).onEnter(task);
        }
        return rows == 1;
    }
}


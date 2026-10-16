package org.example.bigevent.service.impl;

import org.example.bigevent.mapper.PushTaskMapper;
import org.example.bigevent.pojo.PushTask;
import org.example.bigevent.pojo.PushTaskStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * 推送任务状态机（状态模式实现）。
 *
 * <p>设计要点：
 * <ol>
 *   <li>状态与流转规则收敛在 {@link PushTaskStatus}（单一数据源，杜绝 if-else 散落各处）；</li>
 *   <li>每个状态可以注册进入时的副作用处理器（{@link StateHandler}），新增状态只需新增处理器；</li>
 *   <li>落库用乐观锁（{@code WHERE status = ?}），防止并发下两个消费者同时推进同一任务。</li>
 * </ol>
 */
@Component
public class PushTaskStateMachine {

    private static final Logger log = LoggerFactory.getLogger(PushTaskStateMachine.class);

    /** 状态进入处理器：状态模式中「每个状态一个类」的轻量版，副作用都收在这里。 */
    public interface StateHandler {
        /**
         * 进入该状态时执行。
         *
         * @param task 已完成流转的任务
         */
        void onEnter(PushTask task);
    }

    private final Map<PushTaskStatus, StateHandler> handlers = new EnumMap<>(PushTaskStatus.class);
    private final PushTaskMapper mapper;

    /**
     * 构造状态机并注册各状态的进入副作用。
     *
     * @param mapper 推送任务数据访问
     */
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
     * @param task  任务对象，成功后会被就地更新为新的状态与错误信息
     * @param from  期望的当前状态
     * @param to    目标状态
     * @param error 失败原因；成功流转传 {@code null}
     * @return {@code true} 表示流转成功（影响行数=1）；{@code false} 表示并发下状态已被改变，本次流转无效
     * @throws IllegalStateException 状态之间的流转不合法时抛出
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

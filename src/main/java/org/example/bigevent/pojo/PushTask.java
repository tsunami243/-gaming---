package org.example.bigevent.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
/**
 * 推送任务实体：一条待推送消息的持久化状态。
 *
 * <p>状态按 {@link PushTaskStatus} 的状态机流转，{@code messageId} 是幂等键，
 * 失败原因记录在 {@code lastError}。</p>
 */
public class PushTask {
    private Long id;
    /** 消息唯一 ID，同时是幂等 key */
    private String messageId;
    private String userId;
    private String content;
    /** 状态名，对应 {@link PushTaskStatus} */
    private String status;
    private Integer retryCount;
    private String lastError;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}


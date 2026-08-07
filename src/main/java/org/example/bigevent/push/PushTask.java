package org.example.bigevent.push;

import lombok.Data;

import java.time.LocalDateTime;

@Data
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


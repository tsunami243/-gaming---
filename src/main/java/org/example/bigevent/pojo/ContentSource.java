package org.example.bigevent.pojo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 采集来源配置（只读模型）。本模型不存储任何凭据。
 *
 * <p>{@code verificationStatus} 决定该来源能否真实运行（发布门禁）；
 * {@code autoPublishEnabled} 是自动发布的预留开关，当前恒为关闭。</p>
 */
public record ContentSource(
        Long id,
        String code,
        String name,
        String sourceType,
        String baseUrl,
        List<String> allowedDomains,
        List<String> gameCodes,
        String language,
        String adapterKey,
        int scheduleHours,
        int candidateLimit,
        boolean autoPublishEnabled,
        String verificationStatus,
        LocalDateTime lastVerifiedAt
) {}

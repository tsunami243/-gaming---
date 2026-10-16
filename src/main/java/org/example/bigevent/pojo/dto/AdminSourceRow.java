package org.example.bigevent.pojo.dto;

import java.time.LocalDateTime;

/** Safe operational source view: excludes credentials because none are persisted here. */
public record AdminSourceRow(Long id, String code, String name, String sourceType, String language, String adapterKey,
                             Integer scheduleHours, Integer candidateLimit, Boolean autoPublishEnabled, String verificationStatus,
                             LocalDateTime lastVerifiedAt, LocalDateTime lastSuccessAt, LocalDateTime pausedAt, String pauseReason,
                             LocalDateTime updateTime) { }

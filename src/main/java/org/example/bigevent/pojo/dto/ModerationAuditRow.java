package org.example.bigevent.pojo.dto;

import java.time.LocalDateTime;

/** Administrator-only moderation trail; it intentionally excludes user profile and credential fields. */
public record ModerationAuditRow(Long id, Integer articleId, Integer actorUserId, String action,
                                 String reasonCode, String reasonDetail, String beforeStatus,
                                 String afterStatus, Integer revision, LocalDateTime createdAt) { }

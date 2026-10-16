package org.example.bigevent.pojo.dto;

import java.time.LocalDateTime;

/** 采集运行记录行（后台运维视图）：一次采集运行的状态、耗时与各阶段计数。 */
public record AdminIngestRunRow(String runId, Long sourceId, String status, LocalDateTime startedAt, LocalDateTime finishedAt,
                                Integer discoveredCount, Integer processedCount, Integer duplicateCount, Integer reviewCount,
                                Integer rejectedCount, Integer failedCount, Integer dailyCandidateBudgetUsed, String pausedReason) { }

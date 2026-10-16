package org.example.bigevent.pojo.dto;

import java.time.LocalDateTime;
import java.util.List;

/** 公开文章摘要响应（列表项）：不含正文，避免列表拉取大字段。 */
public record ArticleSummaryResponse(
        int id, String title, String summary, List<String> gameCodes, String contentType,
        String readingMode, String workflowStatus, List<String> tags, String sourceName, String sourceLanguage,
        LocalDateTime originalPublishedAt, java.time.LocalDate originalPublishedDate, LocalDateTime ingestedAt, LocalDateTime publishedAt,
        String coverImg) {}

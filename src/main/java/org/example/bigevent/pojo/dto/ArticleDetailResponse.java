package org.example.bigevent.pojo.dto;

import java.time.LocalDateTime;
import java.util.List;

/** 公开文章详情响应：在摘要字段基础上追加原文链接与正文（正文仅在 FULL_TEXT 读法模式下返回）。 */
public record ArticleDetailResponse(
        int id, String title, String summary, List<String> gameCodes, String contentType,
        String readingMode, String workflowStatus, List<String> tags, String sourceName, String sourceLanguage,
        LocalDateTime originalPublishedAt, java.time.LocalDate originalPublishedDate, LocalDateTime ingestedAt, LocalDateTime publishedAt,
        String coverImg, String originalUrl, String content) {}

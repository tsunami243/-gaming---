package org.example.bigevent.pojo.dto;
/** 文章来源名投影行：列表/详情批量补全「文章 → 来源展示名」时使用。 */
public record ArticleSourceRow(Integer articleId, String sourceName) { }

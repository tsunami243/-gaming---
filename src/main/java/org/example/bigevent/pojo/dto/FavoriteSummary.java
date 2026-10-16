package org.example.bigevent.pojo.dto;

import java.time.LocalDateTime;
/** 收藏项摘要：展示我收藏过的文章标题、摘要与收藏时间。 */
public record FavoriteSummary(Integer id, String title, String summary, LocalDateTime savedAt) { }

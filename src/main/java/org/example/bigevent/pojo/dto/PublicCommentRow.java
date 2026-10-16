package org.example.bigevent.pojo.dto;
import java.time.LocalDateTime;
public record PublicCommentRow(Integer id, String displayName, String content, Integer parentId, LocalDateTime createdAt) { }

package org.example.bigevent.pojo.dto;

import java.time.LocalDateTime;

/** 采集回执：eventId 为幂等键，payloadHash 为载荷指纹，articleId 为落库文章。 */
public record IngestReceipt(String eventId, String payloadHash, Integer articleId, String decision, LocalDateTime createdAt) {}

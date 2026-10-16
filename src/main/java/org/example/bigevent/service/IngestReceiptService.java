package org.example.bigevent.service;

import org.example.bigevent.pojo.dto.IngestAcceptance;
import org.example.bigevent.pojo.dto.IngestEvent;

/** 采集事件入库：幂等 receipt + 文章落库事务。 */
public interface IngestReceiptService {

    /** 幂等落库；同一 eventId 与同一指纹返回原结果，指纹不同返回 409。 */
    IngestAcceptance accept(IngestEvent event, String payloadHash);

    /** 事件规范 JSON 的 SHA-256 指纹，用于幂等判定与回执校验。 */
    String payloadHash(IngestEvent event);
}

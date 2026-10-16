package org.example.bigevent.service.impl;
import org.example.bigevent.mapper.IngestContentMapper;
import org.example.bigevent.mapper.IngestReceiptMapper;
import org.example.bigevent.pojo.dto.IngestAcceptance;
import org.example.bigevent.pojo.dto.IngestEvent;
import org.example.bigevent.pojo.dto.IngestReceipt;
import org.example.bigevent.service.IngestReceiptService;

import org.example.bigevent.pojo.Article;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class IngestReceiptServiceTest {
    @Test
    void createsPendingArticleAndReceiptOnce() {
        IngestReceiptMapper receiptMapper = mock(IngestReceiptMapper.class);
        IngestContentMapper contentMapper = mock(IngestContentMapper.class);
        IngestReceiptService service = new IngestReceiptServiceImpl(receiptMapper, contentMapper);
        IngestReceipt receipt = new IngestReceipt("e1", "hash", 77, "REVIEW", null);
        when(receiptMapper.find("e1")).thenReturn(null, receipt);
        when(contentMapper.findArticleIdByCanonicalUrlHash(anyString())).thenReturn(null);
        when(contentMapper.findSourceId("official-cs2")).thenReturn(5L);
        when(contentMapper.findCategoryId("cs2")).thenReturn(9);
        when(contentMapper.findIngestSystemUserId()).thenReturn(1);
        when(contentMapper.findEnabledGameId("cs2")).thenReturn(3);
        doAnswer(invocation -> { ((Article) invocation.getArgument(0)).setId(77); return null; }).when(contentMapper).insertArticle(any());

        assertEquals(new IngestAcceptance(receipt, true), service.accept(event("e1", "REVIEW"), "hash"));
        verify(receiptMapper).insert("e1", "hash", null, "REVIEW", "fps-v1");
        verify(contentMapper).insertArticle(argThat(article -> "草稿".equals(article.getState())
                && "PENDING_REVIEW".equals(article.getWorkflowStatus()) && article.getIngestedAt() != null
                && "private staged content".equals(article.getContent())
                && "FULL_TEXT".equals(article.getReadingMode())));
        verify(contentMapper).insertArticleContent(eq(77), eq("private staged content"), anyString());
        verify(contentMapper).insertArticleGame(77, 3, true);
        verify(receiptMapper).attachArticle("e1", 77);
    }

    @Test
    void reusesExistingArticleForSameCanonicalUrl() {
        IngestReceiptMapper receiptMapper = mock(IngestReceiptMapper.class);
        IngestContentMapper contentMapper = mock(IngestContentMapper.class);
        IngestReceiptService service = new IngestReceiptServiceImpl(receiptMapper, contentMapper);
        when(receiptMapper.find("e1")).thenReturn(null, new IngestReceipt("e1", "hash", 45, "ACCEPT", null));
        when(contentMapper.findArticleIdByCanonicalUrlHash(anyString())).thenReturn(45);

        service.accept(event("e1", "ACCEPT"), "hash");
        verify(contentMapper, never()).insertArticle(any());
        verify(receiptMapper).attachArticle("e1", 45);
    }

    @Test
    void returnsExistingReceiptForSamePayloadAndRejectsConflictingPayload() {
        IngestReceiptMapper receiptMapper = mock(IngestReceiptMapper.class);
        IngestContentMapper contentMapper = mock(IngestContentMapper.class);
        IngestReceiptService service = new IngestReceiptServiceImpl(receiptMapper, contentMapper);
        IngestReceipt receipt = new IngestReceipt("e1", "old", 9, "REVIEW", null);
        when(receiptMapper.find("e1")).thenReturn(receipt);

        assertEquals(new IngestAcceptance(receipt, false), service.accept(event("e1", "REVIEW"), "old"));
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> service.accept(event("e1", "REVIEW"), "new"));
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verifyNoInteractions(contentMapper);
        verify(receiptMapper, never()).insert(any(), any(), any(), any(), any());
    }

    @Test
    void rejectedEventCreatesReceiptWithoutArticle() {
        IngestReceiptMapper receiptMapper = mock(IngestReceiptMapper.class);
        IngestContentMapper contentMapper = mock(IngestContentMapper.class);
        IngestReceiptService service = new IngestReceiptServiceImpl(receiptMapper, contentMapper);
        when(receiptMapper.find("e1")).thenReturn(null, new IngestReceipt("e1", "hash", null, "REJECT", null));

        service.accept(event("e1", "REJECT"), "hash");
        verifyNoInteractions(contentMapper);
        verify(receiptMapper, never()).attachArticle(any(), any());
    }

    private static IngestEvent event(String eventId, String decision) {
        return new IngestEvent(1, eventId, "official-cs2", "CS2 Official", "en", List.of("cs2"), "announcement",
                "https://example.com/news/1", "Title", "Summary", List.of("update"), "private staged content",
                decision, "fps-v1", "2026-09-20T12:00:00Z", "2026-09-20T11:00:00Z", null);
    }
}




package org.example.bigevent.service.impl;
import org.example.bigevent.mapper.AdminArticleMapper;
import org.example.bigevent.pojo.dto.ModerationAuditRow;
import org.example.bigevent.pojo.dto.ModerationRequest;
import org.example.bigevent.service.AdminModerationService;

import org.example.bigevent.pojo.Article;
import org.example.bigevent.utils.ThreadLocalUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminModerationServiceTest {
    private final AdminArticleMapper mapper = mock(AdminArticleMapper.class);
    private final AdminModerationService service = new AdminModerationServiceImpl(mapper);

    @BeforeEach void authenticatedActor() { ThreadLocalUtil.set(Map.of("userId", 42)); }
    @AfterEach void clear() { ThreadLocalUtil.remove(); }

    @Test void approvesWithOptimisticRevisionAndWritesAuditForAuthenticatedActor() {
        Article existing = article(4, "PENDING_REVIEW");
        Article published = article(5, "PUBLISHED");
        when(mapper.findById(7)).thenReturn(existing, published);
        when(mapper.transition(eq(7), eq(4), eq("已发布"), eq("PUBLISHED"), isNull(), any())).thenReturn(1);

        Article result = service.approve(7, new ModerationRequest(4, null, "validated official patch notes"));
        assertSame(published, result);
        verify(mapper).audit(7, 42, "APPROVE", null, "validated official patch notes", "PENDING_REVIEW", "PUBLISHED", 5);
    }

    @Test void rejectsStaleRevisionWithoutChangingArticleOrWritingAudit() {
        when(mapper.findById(7)).thenReturn(article(5, "PENDING_REVIEW"));
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> service.reject(7, new ModerationRequest(4, "LOW_EVIDENCE", "needs review")));
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(mapper, never()).transition(anyInt(), anyInt(), any(), any(), any(), any());
        verify(mapper, never()).audit(anyInt(), anyInt(), any(), any(), any(), any(), any(), anyInt());
    }

    @Test void equalLargeRevisionsAreComparedByValue() {
        when(mapper.findById(7)).thenReturn(article(1024, "PENDING_REVIEW"), article(1025, "PUBLISHED"));
        when(mapper.transition(eq(7), eq(1024), eq("已发布"), eq("PUBLISHED"), isNull(), any())).thenReturn(1);
        assertEquals(1025, service.approve(7, new ModerationRequest(1024, null, null)).getRevision());
        verify(mapper).audit(7, 42, "APPROVE", null, null, "PENDING_REVIEW", "PUBLISHED", 1025);
    }

    @Test void invalidRevisionsNeverReachDatabase() {
        for (Integer revision : java.util.Arrays.asList(null, -1, Integer.MAX_VALUE)) {
            var error = assertThrows(ResponseStatusException.class, () -> service.approve(7, new ModerationRequest(revision, null, null)));
            assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, error.getStatusCode());
        }
        verifyNoInteractions(mapper);
    }

    @Test void refusesModerationWithoutAValidatedActorBeforeAnyDatabaseAccess() {
        ThreadLocalUtil.remove();
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.approve(7, new ModerationRequest(4, null, null)));
        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        verifyNoInteractions(mapper);
    }

    @Test void refusesMalformedOrNonPositiveActorClaimsBeforeAnyDatabaseAccess() {
        for (Object invalidUserId : new Object[]{"42", 0, -4, 42L}) {
            ThreadLocalUtil.set(Map.of("userId", invalidUserId));
            ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.approve(7, new ModerationRequest(4, null, null)));
            assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        }
        verifyNoInteractions(mapper);
    }

    @Test void readsArticleScopedAuditInNewestFirstPageShape() {
        Article article = article(4, "PUBLISHED");
        ModerationAuditRow newest = new ModerationAuditRow(9L, 7, 42, "APPROVE", null, "official notes checked", "PENDING_REVIEW", "PUBLISHED", 4, LocalDateTime.of(2026, 9, 22, 10, 0));
        when(mapper.findById(7)).thenReturn(article);
        when(mapper.listAudit(7)).thenReturn(List.of(newest));

        var page = service.audit(7, 1, 20);

        assertEquals(1, page.total());
        assertEquals(newest, page.items().get(0));
        verify(mapper).findById(7);
        verify(mapper).listAudit(7);
    }

    @Test void auditRejectsUnknownArticleAndInvalidPagingBeforeListingHistory() {
        ResponseStatusException invalidId = assertThrows(ResponseStatusException.class, () -> service.audit(0, 1, 20));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, invalidId.getStatusCode());
        ResponseStatusException invalidPage = assertThrows(ResponseStatusException.class, () -> service.audit(7, 1, 51));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, invalidPage.getStatusCode());
        verifyNoInteractions(mapper);

        when(mapper.findById(7)).thenReturn(null);
        ResponseStatusException missing = assertThrows(ResponseStatusException.class, () -> service.audit(7, 1, 20));
        assertEquals(HttpStatus.NOT_FOUND, missing.getStatusCode());
        verify(mapper, never()).listAudit(anyInt());
    }

    private static Article article(int revision, String status) { Article article = new Article(); article.setId(7); article.setRevision(revision); article.setWorkflowStatus(status); return article; }
}


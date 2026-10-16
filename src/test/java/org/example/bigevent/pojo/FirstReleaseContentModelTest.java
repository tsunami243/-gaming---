package org.example.bigevent.pojo;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FirstReleaseContentModelTest {

    @Test
    void articleDefaultsToFullTextReadingAndDraftWorkflow() {
        Article article = new Article();
        assertEquals(ReadingMode.FULL_TEXT.name(), article.getReadingMode());
        assertEquals(WorkflowStatus.DRAFT.name(), article.getWorkflowStatus());
        assertEquals(0, article.getRevision());
    }

    @Test
    void articleRetainsSourceAndOriginalPublicationMetadata() {
        Article article = new Article();
        LocalDateTime publishedAt = LocalDateTime.of(2026, 9, 20, 10, 30);
        article.setSourceId(7L);
        article.setOriginalUrl("https://example.invalid/cs2-update");
        article.setOriginalPublishedAt(publishedAt);
        article.setContentType(ContentType.PATCH_NOTES.getCode());

        assertEquals(7L, article.getSourceId());
        assertEquals("https://example.invalid/cs2-update", article.getOriginalUrl());
        assertEquals(publishedAt, article.getOriginalPublishedAt());
        assertEquals("patch_notes", article.getContentType());
        assertNull(article.getSummary());
    }

    @Test
    void gameAndSourceRecordsUseStableCodesWithoutCredentials() {
        Game game = new Game(1, "cs2", "Counter-Strike 2", "Counter-Strike 2", List.of("CS2"), true);
        ContentSource source = new ContentSource(1L, "cs2-official", "CS2 Official", "OFFICIAL",
                "https://example.invalid", List.of("example.invalid"), List.of("cs2"), "en",
                "official-api", 6, 20, false, "CANDIDATE", null);

        assertEquals("cs2", game.code());
        assertEquals("cs2-official", source.code());
        assertEquals(List.of("cs2"), source.gameCodes());
    }
}

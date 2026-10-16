package org.example.bigevent.service.impl;

import org.example.bigevent.mapper.ArticleMapper;
import org.example.bigevent.mapper.ArticleSourceMapper;
import org.example.bigevent.mapper.GameMapper;
import org.example.bigevent.pojo.Article;
import org.example.bigevent.pojo.dto.ArticleDetailResponse;
import org.example.bigevent.pojo.dto.ArticleGameRow;
import org.example.bigevent.pojo.dto.ArticleSourceRow;
import org.example.bigevent.pojo.dto.ArticleSummaryResponse;
import org.example.bigevent.pojo.dto.GameRow;
import org.example.bigevent.pojo.dto.PageResponse;
import org.example.bigevent.service.PublicArticleReadService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Business rules for public reads: whitelists, batching, and stored-body text extraction. */
class PublicArticleReadServiceImplTest {

    private final GameMapper gameMapper = mock(GameMapper.class);
    private final ArticleMapper articleMapper = mock(ArticleMapper.class);
    private final ArticleSourceMapper sourceMapper = mock(ArticleSourceMapper.class);
    private final PublicArticleReadService service = new PublicArticleReadServiceImpl(gameMapper, articleMapper, sourceMapper);

    @Test
    void gamesExposeParsedAliasesAndEnabledFlag() {
        when(gameMapper.findEnabled()).thenReturn(List.of(
                new GameRow(1, "cs2", "Counter-Strike 2", "Counter-Strike 2", "[\"CS2\",\"反恐精英2\"]", true),
                new GameRow(2, "valorant", "无畏契约", "VALORANT", "[\"无畏契约\"]", false)));

        var games = service.listGames();

        assertEquals(2, games.size());
        assertEquals(List.of("CS2", "反恐精英2"), games.get(0).aliases());
        assertTrue(games.get(0).enabled());
        assertFalse(games.get(1).enabled());
    }

    @Test
    void rejectsPageSizeAboveThePublicMaximum() {
        ResponseStatusException tooLarge = assertThrows(ResponseStatusException.class,
                () -> service.listArticles(1, 51, null, null));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, tooLarge.getStatusCode());

        ResponseStatusException tooSmall = assertThrows(ResponseStatusException.class,
                () -> service.listArticles(0, 20, null, null));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, tooSmall.getStatusCode());
        verifyNoInteractions(articleMapper);
    }

    @Test
    void rejectsGameAndContentTypeOutsideTheWhitelists() {
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, assertThrows(ResponseStatusException.class,
                () -> service.listArticles(1, 20, "minecraft", null)).getStatusCode());
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, assertThrows(ResponseStatusException.class,
                () -> service.listArticles(1, 20, null, "hardware")).getStatusCode());
        verifyNoInteractions(articleMapper);
    }

    @Test
    void blankFiltersAreSentAsNullSoTheSqlSkipsThem() {
        when(articleMapper.listPublishedByFilters(null, null)).thenReturn(List.of());

        service.listArticles(1, 20, "  ", "");

        verify(articleMapper).listPublishedByFilters(null, null);
    }

    @Test
    void listBatchesGamesAndSourcesOnceForTheWholePage() {
        Article first = article(7);
        Article second = article(8);
        when(articleMapper.listPublishedByFilters("valorant", null)).thenReturn(List.of(first, second));
        when(gameMapper.findArticleGames(List.of(7, 8))).thenReturn(List.of(
                new ArticleGameRow(7, "cs2"), new ArticleGameRow(7, "valorant"), new ArticleGameRow(8, "valorant")));
        when(sourceMapper.findNames(List.of(7, 8))).thenReturn(List.of(
                new ArticleSourceRow(7, "官方来源"), new ArticleSourceRow(8, "官方来源")));

        PageResponse<ArticleSummaryResponse> page = service.listArticles(1, 20, "valorant", null);

        assertEquals(2, page.total());
        assertEquals(List.of("cs2", "valorant"), page.items().get(0).gameCodes());
        assertEquals(List.of("valorant"), page.items().get(1).gameCodes());
        assertEquals("官方来源", page.items().get(0).sourceName());
        verify(gameMapper, times(1)).findArticleGames(anyList());
        verify(sourceMapper, times(1)).findNames(anyList());
    }

    @Test
    void emptyPageDoesNotIssueEmptyInQuery() {
        when(articleMapper.listPublishedByFilters(null, null)).thenReturn(List.of());

        PageResponse<ArticleSummaryResponse> page = service.listArticles(1, 20, null, null);

        assertTrue(page.items().isEmpty());
        assertEquals(0, page.total());
        verify(gameMapper, never()).findArticleGames(any());
        verify(sourceMapper, never()).findNames(any());
    }

    @Test
    void summaryFallsBackToStrippedContentWhenSummaryIsBlank() {
        Article blank = article(7);
        blank.setSummary("   ");
        blank.setContent("<p>Fallback text from the stored body.</p>");
        when(articleMapper.selectPublishedContentById(7)).thenReturn(blank);
        when(gameMapper.findArticleGames(List.of(7))).thenReturn(List.of());
        when(sourceMapper.findNames(List.of(7))).thenReturn(List.of());

        ArticleDetailResponse detail = service.articleDetail(7);

        assertEquals("Fallback text from the stored body.", detail.summary());
    }

    @Test
    void missingArticleMapsToNotFound() {
        when(articleMapper.selectPublishedContentById(9)).thenReturn(null);

        ResponseStatusException missing = assertThrows(ResponseStatusException.class, () -> service.articleDetail(9));

        assertEquals(HttpStatus.NOT_FOUND, missing.getStatusCode());
    }

    @Test
    void fullTextDetailServesTheStoredBodyAsPlainText() {
        Article stored = article(7);
        stored.setReadingMode("FULL_TEXT");
        stored.setContent("<h2>INTRO</h2><p>First &amp; second.</p><p>Third &lt;tag&gt;.</p>");
        when(articleMapper.selectPublishedContentById(7)).thenReturn(stored);
        when(gameMapper.findArticleGames(List.of(7))).thenReturn(List.of(new ArticleGameRow(7, "cs2")));
        when(sourceMapper.findNames(List.of(7))).thenReturn(List.of(new ArticleSourceRow(7, "官方来源")));

        ArticleDetailResponse detail = service.articleDetail(7);

        assertEquals("INTRO\n\nFirst & second.\n\nThird <tag>.", detail.content());
        assertFalse(detail.content().contains("</p>"), "stored block markup must never reach the public response");
        assertFalse(detail.content().contains("<h2>"), "stored heading markup must never reach the public response");
        assertEquals("FULL_TEXT", detail.readingMode());
        assertEquals(List.of("cs2"), detail.gameCodes());
        assertEquals("https://example.invalid/cs2", detail.originalUrl());
    }

    @Test
    void summaryReadingModeStillHidesTheStoredBody() {
        Article stored = article(7);
        stored.setReadingMode("EXTERNAL_SUMMARY");
        stored.setContent("<p>Stored body</p>");
        when(articleMapper.selectPublishedContentById(7)).thenReturn(stored);
        when(gameMapper.findArticleGames(List.of(7))).thenReturn(List.of());
        when(sourceMapper.findNames(List.of(7))).thenReturn(List.of());

        assertNull(service.articleDetail(7).content());
    }

    @Test
    void blankStoredBodyIsNotExposed() {
        Article stored = article(7);
        stored.setReadingMode("FULL_TEXT");
        stored.setContent("   ");
        when(articleMapper.selectPublishedContentById(7)).thenReturn(stored);
        when(gameMapper.findArticleGames(List.of(7))).thenReturn(List.of());
        when(sourceMapper.findNames(List.of(7))).thenReturn(List.of());

        assertNull(service.articleDetail(7).content());
    }

    @Test
    void invalidIdIsRejectedBeforeAnyQuery() {
        assertThrows(ResponseStatusException.class, () -> service.articleDetail(0));
        verifyNoInteractions(articleMapper);
    }

    private Article article(int id) {
        Article article = new Article();
        article.setId(id);
        article.setTitle("CS2 更新");
        article.setSummary("CS2 更新摘要");
        article.setTags("[\"CS2\",\"更新\"]");
        article.setContentType("patch_notes");
        article.setSourceLanguage("en");
        article.setOriginalUrl("https://example.invalid/cs2");
        article.setWorkflowStatus("PUBLISHED");
        return article;
    }
}

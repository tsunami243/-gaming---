package org.example.bigevent.service.impl;

import org.example.bigevent.mapper.ArticleMapper;
import org.example.bigevent.pojo.Article;
import org.example.bigevent.pojo.PageBean;
import org.example.bigevent.utils.ThreadLocalUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArticleVisibilityServiceTest {

    private final ArticleMapper articleMapper = mock(ArticleMapper.class);
    private final ArticleServicelmpl service = new ArticleServicelmpl();

    ArticleVisibilityServiceTest() {
        ReflectionTestUtils.setField(service, "articleMapper", articleMapper);
    }

    @AfterEach
    void clearThreadLocal() {
        ThreadLocalUtil.remove();
    }

    @Test
    void publicListIgnoresCallerProvidedDraftState() {
        when(articleMapper.list(eq(9), eq("已发布"), eq(null))).thenReturn(List.of());

        PageBean<Article> page = service.list(1, 20, 9, "草稿");

        assertEquals(0L, page.getTotal());
        verify(articleMapper).list(9, "已发布", null);
    }

    @Test
    void publicDetailHidesDraftAndRequirePublishedReturnsNotFound() {
        when(articleMapper.detailPublished(42)).thenReturn(null);

        assertNull(service.detail(42));
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.requirePublished(42));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void contentAdminCanUpdateSystemOwnedArticle() {
        Article existing = new Article();
        existing.setId(7);
        existing.setCreateUser(1);
        Article update = new Article();
        update.setId(7);
        update.setTitle("更新后的标题");
        update.setContent("更新后的正文内容");
        update.setState("草稿");
        update.setCategoryId(9);
        when(articleMapper.detail(7)).thenReturn(existing);
        ThreadLocalUtil.set(Map.of("userId", 8, "role", "ROLE_CONTENT_ADMIN"));

        service.update(update);

        ArgumentCaptor<Article> captor = ArgumentCaptor.forClass(Article.class);
        verify(articleMapper).updateByAdmin(captor.capture());
        assertEquals(1, captor.getValue().getCreateUser());
    }
}

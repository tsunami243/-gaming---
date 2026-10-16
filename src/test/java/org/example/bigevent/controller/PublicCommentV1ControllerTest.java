package org.example.bigevent.controller;
import org.example.bigevent.mapper.PublicCommentMapper;
import org.example.bigevent.pojo.dto.PublicCommentRow;
import org.example.bigevent.service.ArticleService;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class PublicCommentV1ControllerTest {
    @Test void unpublishedArticleStopsBeforeCommentRead() {
        var mapper=mock(PublicCommentMapper.class); var articles=mock(ArticleService.class);
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND)).when(articles).requirePublished(7);
        assertThrows(ResponseStatusException.class,()->new PublicCommentV1Controller(mapper,articles).list(7,1,20));
        verifyNoInteractions(mapper);
    }
    @Test void publishedArticleReturnsBoundedPage() {
        var mapper=mock(PublicCommentMapper.class); var articles=mock(ArticleService.class);
        when(mapper.page(7,20,20L)).thenReturn(List.of(new PublicCommentRow(1,"user","text",null,null)));
        when(mapper.count(7)).thenReturn(21L);
        var page=new PublicCommentV1Controller(mapper,articles).list(7,2,20).data();
        assertEquals(21,page.total()); assertEquals(1,page.items().size());
        verify(articles).requirePublished(7);
    }
    @Test void oversizedPageRejectedBeforeServiceAccess() {
        var mapper=mock(PublicCommentMapper.class); var articles=mock(ArticleService.class);
        assertThrows(ResponseStatusException.class,()->new PublicCommentV1Controller(mapper,articles).list(7,1,51));
        verifyNoInteractions(mapper,articles);
    }
}

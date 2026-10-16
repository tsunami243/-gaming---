package org.example.bigevent.service.impl;

import org.example.bigevent.mapper.CommentMapper;
import org.example.bigevent.pojo.Comment;
import org.example.bigevent.service.HeatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CommentWriteValidationTest {
    CommentMapper mapper = mock(CommentMapper.class);
    HeatService heat = mock(HeatService.class);
    CommentServiceImpl service = new CommentServiceImpl();
    @BeforeEach void setup() {
        ReflectionTestUtils.setField(service,"commentMapper",mapper);
        ReflectionTestUtils.setField(service,"heatService",heat);
    }
    Comment valid() {
        Comment comment = new Comment(); comment.setArticleId(7); comment.setUserId(3); comment.setContent("正文"); return comment;
    }
    @Test void rejectsEmptyAndOversizedContentWithoutWrites() {
        for (String content : java.util.Arrays.asList(null, "", "  \n", "x".repeat(1001))) {
            Comment comment = valid(); comment.setContent(content);
            assertThrows(ResponseStatusException.class, () -> service.addComment(comment));
        }
        verifyNoInteractions(mapper, heat);
    }
    @Test void rejectsCrossArticleParentAndReplyWithoutWrites() {
        Comment foreign = valid(); foreign.setArticleId(99);
        when(mapper.detail(10)).thenReturn(foreign);
        Comment child = valid(); child.setParentId(10);
        assertThrows(ResponseStatusException.class, () -> service.addComment(child));
        child.setParentId(null); child.setReplyId(10);
        assertThrows(ResponseStatusException.class, () -> service.addComment(child));
        verify(mapper, never()).add(any()); verifyNoInteractions(heat);
    }
    @Test void rejectsMissingParentWithoutWriting() {
        Comment child = valid(); child.setParentId(11);
        assertThrows(ResponseStatusException.class, () -> service.addComment(child));
        verify(mapper, never()).add(any()); verifyNoInteractions(heat);
    }
    @Test void permitsBoundedCommentReplyOnSameArticle() {
        when(mapper.detail(10)).thenReturn(valid());
        Comment child = valid(); child.setParentId(10); child.setReplyId(10); child.setContent("x".repeat(1000));
        service.addComment(child);
        verify(mapper).add(child); verify(heat).incr(7, HeatService.DELTA_COMMENT);
        assertNotNull(child.getCreateTime());
    }
}

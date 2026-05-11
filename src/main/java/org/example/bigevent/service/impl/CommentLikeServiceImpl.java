package org.example.bigevent.service.impl;

import org.example.bigevent.mapper.CommentLikeMapper;
import org.example.bigevent.pojo.CommentLike;
import org.example.bigevent.service.CommentLikeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CommentLikeServiceImpl implements CommentLikeService {
    @Autowired
    private CommentLikeMapper commentLikeMapper;

    @Override
    public void addLike(Integer commentId, Integer userId) {
        CommentLike commentLike = new CommentLike(commentId, userId);
        commentLike.setCreateTime(LocalDateTime.now());
        commentLikeMapper.add(commentLike);
    }

    @Override
    public void deleteLike(Integer commentId, Integer userId) {
        commentLikeMapper.delete(commentId, userId);
    }

    @Override
    public List<CommentLike> listByUserId(Integer userId) {
        return commentLikeMapper.listByUserId(userId);
    }

    @Override
    public Boolean hasLiked(Integer commentId, Integer userId) {
        CommentLike commentLike = commentLikeMapper.findByCommentIdAndUserId(commentId, userId);
        return commentLike != null;
    }

    @Override
    public Integer countByCommentId(Integer commentId) {
        return commentLikeMapper.countByCommentId(commentId);
    }
}

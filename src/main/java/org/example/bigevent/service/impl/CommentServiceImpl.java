package org.example.bigevent.service.impl;

import org.example.bigevent.mapper.CommentMapper;
import org.example.bigevent.pojo.Comment;
import org.example.bigevent.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CommentServiceImpl implements CommentService {
    @Autowired
    private CommentMapper commentMapper;

    @Override
    public void addComment(Comment comment) {
        comment.setCreateTime(LocalDateTime.now());
        comment.setUpdateTime(LocalDateTime.now());
        commentMapper.add(comment);
    }

    @Override
    public List<Comment> listByArticleId(Integer articleId) {
        return commentMapper.listByArticleId(articleId);
    }

    @Override
    public List<Comment> listArticleCommentTree(Integer articleId) {
        // 获取顶级评论（parent_id为null）
        List<Comment> topLevelComments = commentMapper.listTopLevelByArticleId(articleId);

        // 获取所有回复（parent_id不为null）
        List<Comment> allReplies = commentMapper.listByArticleId(articleId);

        // 按parentId分组
        Map<Integer, List<Comment>> replyMap = new HashMap<>();
        for (Comment reply : allReplies) {
            if (reply.getParentId() != null) {
                replyMap.computeIfAbsent(reply.getParentId(), k -> new ArrayList<>()).add(reply);
            }
        }

        // 为每个顶级评论设置其回复
        for (Comment topComment : topLevelComments) {
            List<Comment> replies = replyMap.get(topComment.getId());
            if (replies != null) {
                topComment.setReplies(replies);
            } else {
                topComment.setReplies(new ArrayList<>());
            }
        }

        return topLevelComments;
    }

    @Override
    public Comment detail(Integer id) {
        return commentMapper.detail(id);
    }

    @Override
    public void delete(Integer id, Integer userId) {
        commentMapper.delete(id, userId);
    }

    @Override
    public void updateLikeCount(Integer id, Integer likeCount) {
        commentMapper.updateLikeCount(id, likeCount);
    }
}

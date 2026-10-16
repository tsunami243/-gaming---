package org.example.bigevent.service;

import org.example.bigevent.pojo.Comment;

import java.util.List;

/** 评论业务：新增、按文章查询、评论树组装与删除。 */
public interface CommentService {

    /**
     * 新增评论（校验字段与父子文章一致性）。
     *
     * @param comment 评论实体
     */
    void addComment(Comment comment);

    /**
     * 查询某篇文章下的全部评论（扁平列表）。
     *
     * @param articleId 文章 ID
     * @return 评论列表
     */
    List<Comment> listByArticleId(Integer articleId);

    /**
     * 查询某篇文章的评论树：顶级评论 + 各自挂载的回复列表。
     *
     * @param articleId 文章 ID
     * @return 顶级评论列表，{@code replies} 已填充
     */
    List<Comment> listArticleCommentTree(Integer articleId);

    /**
     * 查询单条评论详情。
     *
     * @param id 评论 ID
     * @return 评论实体，不存在时返回 {@code null}
     */
    Comment detail(Integer id);

    /**
     * 删除评论（仅作者本人可删）。
     *
     * @param id     评论 ID
     * @param userId 当前登录用户 ID
     */
    void delete(Integer id, Integer userId);

    /**
     * 更新评论点赞数（由点赞服务调用）。
     *
     * @param id        评论 ID
     * @param likeCount 目标点赞数
     */
    void updateLikeCount(Integer id, Integer likeCount);
}

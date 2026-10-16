package org.example.bigevent.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.bigevent.pojo.dto.PublicCommentRow;

import java.util.List;

/**
 * 公开评论读取：只返回已发布文章下的评论。
 */
@Mapper
public interface PublicCommentMapper {

    /** 列表与计数的公共 FROM/WHERE 片段：文章未发布时其评论一并不可见。 */
    String FROM = " from comment c join article a on a.id=c.article_id where c.article_id=#{articleId} and a.state='已发布' and a.workflow_status='PUBLISHED'";

    /**
     * 分页查询评论，按时间倒序。
     *
     * @param articleId 文章 ID
     * @param limit     每页条数
     * @param offset    偏移量
     * @return 评论行列表，用户名为空时以「用户」占位
     */
    @Select("select c.id,coalesce(c.user_name,'用户') as displayName,c.content,c.parent_id as parentId,c.create_time as createdAt" + FROM + " order by c.create_time desc,c.id desc limit #{limit} offset #{offset}")
    List<PublicCommentRow> page(@Param("articleId") int articleId, @Param("limit") int limit, @Param("offset") long offset);

    /**
     * 统计评论总数。
     *
     * @param articleId 文章 ID
     * @return 评论总数
     */
    @Select("select count(*)" + FROM)
    long count(@Param("articleId") int articleId);
}

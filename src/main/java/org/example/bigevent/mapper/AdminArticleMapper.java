package org.example.bigevent.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.bigevent.pojo.Article;
import org.example.bigevent.pojo.dto.ModerationAuditRow;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审核台数据访问：文章读取、带乐观锁的状态流转与审核审计写入。
 *
 * <p>区别于公开读取用的 {@code ArticleMapper}：本接口不做「已发布」过滤，
 * 需要看到全部状态的稿件；其调用方必须已完成管理员鉴权。</p>
 */
@Mapper
public interface AdminArticleMapper {

    /**
     * 按 ID 读取文章（不限状态）。
     *
     * @param id 文章 ID
     * @return 文章实体，不存在时返回 {@code null}
     */
    @Select("select * from article where id=#{id}")
    Article findById(@Param("id") int id);

    /**
     * 按审核状态列出文章，按入库时间倒序。
     *
     * @param workflowStatus 审核状态过滤，为空表示全部
     * @return 文章列表
     */
    @Select("<script>select * from article <where><if test='workflowStatus != null and workflowStatus != &quot;&quot;'>workflow_status=#{workflowStatus}</if></where> order by ingested_at desc, id desc</script>")
    List<Article> list(@Param("workflowStatus") String workflowStatus);

    /**
     * 查询某篇文章的审核操作记录，按时间倒序。
     *
     * @param articleId 文章 ID
     * @return 审计记录列表
     */
    @Select("select id,article_id as articleId,actor_user_id as actorUserId,action,reason_code as reasonCode,reason_detail as reasonDetail,before_status as beforeStatus,after_status as afterStatus,revision,created_at as createdAt from moderation_audit where article_id=#{articleId} order by created_at desc,id desc")
    List<ModerationAuditRow> listAudit(@Param("articleId") int articleId);

    /**
     * 带乐观锁地流转文章状态：仅当库中 revision 与传入值一致时才更新，并把 revision 加一。
     *
     * @param id             文章 ID
     * @param revision       期望的当前版本号（乐观锁）
     * @param state          legacy 发布状态
     * @param workflowStatus 新的审核状态
     * @param reasonCode     原因码（可为 null）
     * @param publishedAt    发布时间（非发布动作可为 null）
     * @return 受影响行数；返回 0 表示版本冲突，流转未生效
     */
    @Update("update article set state=#{state},workflow_status=#{workflowStatus},workflow_reason_code=#{reasonCode},published_at=#{publishedAt},revision=revision+1,update_time=now() where id=#{id} and revision=#{revision}")
    int transition(@Param("id") int id, @Param("revision") int revision, @Param("state") String state,
                   @Param("workflowStatus") String workflowStatus, @Param("reasonCode") String reasonCode,
                   @Param("publishedAt") LocalDateTime publishedAt);

    /**
     * 写入一条审核审计记录。
     *
     * @param articleId    文章 ID
     * @param actorUserId  操作人用户 ID
     * @param action       动作（approve / reject / unpublish）
     * @param reasonCode   原因码
     * @param reasonDetail 原因说明
     * @param beforeStatus 流转前状态
     * @param afterStatus  流转后状态
     * @param revision     本次操作对应的版本号
     */
    @Insert("insert into moderation_audit(article_id,actor_user_id,action,reason_code,reason_detail,before_status,after_status,revision) values(#{articleId},#{actorUserId},#{action},#{reasonCode},#{reasonDetail},#{beforeStatus},#{afterStatus},#{revision})")
    void audit(@Param("articleId") int articleId, @Param("actorUserId") Integer actorUserId, @Param("action") String action,
               @Param("reasonCode") String reasonCode, @Param("reasonDetail") String reasonDetail,
               @Param("beforeStatus") String beforeStatus, @Param("afterStatus") String afterStatus, @Param("revision") int revision);
}

package org.example.bigevent.service;

import org.example.bigevent.pojo.Article;
import org.example.bigevent.pojo.dto.ModerationAuditRow;
import org.example.bigevent.pojo.dto.ModerationRequest;
import org.example.bigevent.pojo.dto.PageResponse;

/** 审核台：只读列表与文章状态流转（乐观锁 + 操作审计）。 */
public interface AdminModerationService {

    /**
     * 分页查询文章（可按审核状态筛选）。
     *
     * @param pageNum        页码，从 1 开始
     * @param pageSize       每页条数
     * @param workflowStatus 审核状态过滤，为空表示不筛选
     * @return 分页结果
     */
    PageResponse<Article> list(int pageNum, int pageSize, String workflowStatus);

    /**
     * 分页查询某篇文章的审核操作记录。
     *
     * @param articleId 文章 ID
     * @param pageNum   页码
     * @param pageSize  每页条数
     * @return 审核记录分页结果
     */
    PageResponse<ModerationAuditRow> audit(int articleId, int pageNum, int pageSize);

    /**
     * 审核通过：文章转为已发布，并写入审计记录。
     *
     * @param id      文章 ID
     * @param request 含乐观锁版本号与原因码
     * @return 流转后的文章
     */
    Article approve(int id, ModerationRequest request);

    /**
     * 审核驳回：文章转为已驳回，并写入审计记录。
     *
     * @param id      文章 ID
     * @param request 含乐观锁版本号与原因码
     * @return 流转后的文章
     */
    Article reject(int id, ModerationRequest request);

    /**
     * 下架：已发布文章转为已下架，并写入审计记录。
     *
     * @param id      文章 ID
     * @param request 含乐观锁版本号与原因码
     * @return 流转后的文章
     */
    Article unpublish(int id, ModerationRequest request);
}

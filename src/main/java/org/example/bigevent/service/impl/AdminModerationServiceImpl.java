package org.example.bigevent.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.example.bigevent.mapper.AdminArticleMapper;
import org.example.bigevent.pojo.Article;
import org.example.bigevent.pojo.WorkflowStatus;
import org.example.bigevent.pojo.dto.ModerationAuditRow;
import org.example.bigevent.pojo.dto.ModerationRequest;
import org.example.bigevent.pojo.dto.PageResponse;
import org.example.bigevent.service.AdminModerationService;
import org.example.bigevent.utils.ThreadLocalUtil;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 内容审核（管理员）的业务实现。
 *
 * <p>三条铁律：
 * <ol>
 *   <li><b>可归因</b>——每次审核动作都必须能对应到 JWT 中的管理员 ID，
 *       否则拒绝执行，绝不写入 actor 为空的审计行；</li>
 *   <li><b>乐观锁</b>——以 {@code revision} 做并发保护，过期即返回 409，
 *       避免两个管理员相互覆盖；</li>
 *   <li><b>可追溯</b>——状态变更与审计写入在同一个事务内完成，
 *       审计失败则状态一并回滚。</li>
 * </ol>
 */
@Service
public class AdminModerationServiceImpl implements AdminModerationService {

    /** 管理端列表允许筛选的工作流状态。 */
    private static final Set<String> LISTABLE = Set.of("PENDING_REVIEW", "PUBLISHED", "REJECTED", "UNPUBLISHED");

    private final AdminArticleMapper mapper;

    /**
     * 构造审核服务。
     *
     * @param mapper 审核相关的文章读写
     */
    public AdminModerationServiceImpl(AdminArticleMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 分页查询待审核/已发布的文章。
     *
     * @param pageNum        页码，从 1 开始
     * @param pageSize       每页条数，1–50
     * @param workflowStatus 可选的工作流状态筛选
     * @return 文章分页
     * @throws ResponseStatusException 分页越界或工作流状态不在白名单时返回 422
     */
    @Override
    public PageResponse<Article> list(int pageNum, int pageSize, String workflowStatus) {
        validatePage(pageNum, pageSize);
        if (workflowStatus != null && !workflowStatus.isBlank() && !LISTABLE.contains(workflowStatus)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "unsupported workflowStatus");
        }
        PageHelper.startPage(pageNum, pageSize);
        List<Article> articles = mapper.list(blankToNull(workflowStatus));
        long total = articles instanceof Page<?> page ? page.getTotal() : articles.size();
        return new PageResponse<>(articles, total, pageNum, pageSize);
    }

    /**
     * 查询某篇文章的审核历史（只读，按时间倒序分页）。
     *
     * <p>刻意只返回管理员 ID 而非关联用户资料，避免把账号信息带进审核视图。
     *
     * @param articleId 文章 ID
     * @param pageNum   页码，从 1 开始
     * @param pageSize  每页条数，1–50
     * @return 审计记录分页
     * @throws ResponseStatusException 文章 ID 非法或分页越界返回 422；文章不存在返回 404
     */
    @Override
    public PageResponse<ModerationAuditRow> audit(int articleId, int pageNum, int pageSize) {
        if (articleId < 1) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "invalid article id");
        }
        validatePage(pageNum, pageSize);
        if (mapper.findById(articleId) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "文章不存在");
        }
        PageHelper.startPage(pageNum, pageSize);
        List<ModerationAuditRow> entries = mapper.listAudit(articleId);
        long total = entries instanceof Page<?> page ? page.getTotal() : entries.size();
        return new PageResponse<>(entries, total, pageNum, pageSize);
    }

    /**
     * 审核通过：{@code PENDING_REVIEW -> PUBLISHED}，并记录站内发布时间。
     *
     * @param id      文章 ID
     * @param request 审核请求，{@code revision} 必填且须与当前一致
     * @return 更新后的文章
     */
    @Override
    @Transactional
    public Article approve(int id, ModerationRequest request) {
        return transition(id, request, "APPROVE", WorkflowStatus.PUBLISHED, "已发布",
                LocalDateTime.now(), Set.of(WorkflowStatus.PENDING_REVIEW.name()));
    }

    /**
     * 审核拒绝：{@code PENDING_REVIEW -> REJECTED}。
     *
     * @param id      文章 ID
     * @param request 审核请求，{@code revision} 必填且须与当前一致
     * @return 更新后的文章
     */
    @Override
    @Transactional
    public Article reject(int id, ModerationRequest request) {
        return transition(id, request, "REJECT", WorkflowStatus.REJECTED, "草稿",
                null, Set.of(WorkflowStatus.PENDING_REVIEW.name()));
    }

    /**
     * 下架已发布文章：{@code PUBLISHED -> UNPUBLISHED}。
     *
     * @param id      文章 ID
     * @param request 审核请求，{@code revision} 必填且须与当前一致
     * @return 更新后的文章
     */
    @Override
    @Transactional
    public Article unpublish(int id, ModerationRequest request) {
        return transition(id, request, "UNPUBLISH", WorkflowStatus.UNPUBLISHED, "草稿",
                null, Set.of(WorkflowStatus.PUBLISHED.name()));
    }

    /**
     * 三种审核动作的公共流程：校验身份与入参 → 校验当前状态与版本 → 落库 → 写审计。
     *
     * <p>身份校验放在所有数据库交互之前：凭据缺失或畸形时不得产生状态变更，
     * 也不得写入 actor 为空的审计行。
     *
     * @param id            文章 ID
     * @param request       审核请求
     * @param action        审计动作名（APPROVE / REJECT / UNPUBLISH）
     * @param target        目标工作流状态
     * @param state         旧模型的展示状态（如「已发布」「草稿」）
     * @param publishedAt   站内发布时间；无需发布时传 {@code null}
     * @param allowedBefore 允许执行该动作的前置状态集合
     * @return 流转后的文章
     * @throws ResponseStatusException 身份无效 401；入参非法 422；文章不存在 404；版本或状态冲突 409
     */
    private Article transition(int id, ModerationRequest request, String action, WorkflowStatus target,
                               String state, LocalDateTime publishedAt, Set<String> allowedBefore) {
        int actorUserId = currentActorUserId();

        if (request == null || request.revision() == null || request.revision() < 0
                || request.revision() == Integer.MAX_VALUE) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "revision must be between 0 and 2147483646");
        }
        Article existing = mapper.findById(id);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "文章不存在");
        }
        if (!allowedBefore.contains(existing.getWorkflowStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "文章当前状态不允许该审核动作");
        }
        if (existing.getRevision() == null || !existing.getRevision().equals(request.revision())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "文章已被其他管理员修改，请刷新后重试");
        }

        int updated = mapper.transition(id, request.revision(), state, target.name(), request.reasonCode(), publishedAt);
        if (updated != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "文章已被其他管理员修改，请刷新后重试");
        }

        int nextRevision = request.revision() + 1;
        mapper.audit(id, actorUserId, action, request.reasonCode(), request.reasonDetail(),
                existing.getWorkflowStatus(), target.name(), nextRevision);

        Article changed = mapper.findById(id);
        if (changed == null) {
            throw new IllegalStateException("article disappeared after moderation transition");
        }
        return changed;
    }

    /**
     * 取出当前登录管理员的 ID。
     *
     * @return 管理员用户 ID
     * @throws ResponseStatusException 登录态缺失或凭据畸形时返回 401
     */
    private int currentActorUserId() {
        Object rawClaims = ThreadLocalUtil.get();
        if (!(rawClaims instanceof Map<?, ?> claims) || !(claims.get("userId") instanceof Integer userId) || userId <= 0) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "有效的管理员身份凭据是必需的");
        }
        return userId;
    }

    /**
     * 校验分页参数。
     *
     * @param pageNum  页码，须 ≥ 1
     * @param pageSize 每页条数，须在 1–50
     * @throws ResponseStatusException 越界时返回 422
     */
    private static void validatePage(int pageNum, int pageSize) {
        if (pageNum < 1 || pageSize < 1 || pageSize > 50) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "invalid page");
        }
    }

    /** 空白字符串归一为 {@code null}，让 SQL 跳过该筛选条件。 */
    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}

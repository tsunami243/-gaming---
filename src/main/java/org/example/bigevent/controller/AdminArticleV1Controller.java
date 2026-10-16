package org.example.bigevent.controller;

import jakarta.validation.Valid;
import org.example.bigevent.filter.ApiTraceIdFilter;
import org.example.bigevent.pojo.Article;
import org.example.bigevent.pojo.dto.ApiResponse;
import org.example.bigevent.pojo.dto.ModerationAuditRow;
import org.example.bigevent.pojo.dto.ModerationRequest;
import org.example.bigevent.pojo.dto.PageResponse;
import org.example.bigevent.service.AdminModerationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端 · 文章审核（v1）。
 *
 * <p>基础路径 {@code /api/v1/admin/articles}；需 CONTENT_ADMIN / SUPER_ADMIN 角色。
 * 状态流转基于 revision 乐观锁，每次流转都会写入审核审计记录。
 */
@RestController
@RequestMapping("/api/v1/admin/articles")
public class AdminArticleV1Controller {

    private final AdminModerationService service;

    /**
     * 构造控制器。
     *
     * @param service 审核台服务
     */
    public AdminArticleV1Controller(AdminModerationService service) {
        this.service = service;
    }

    /**
     * 审核列表（分页）。
     *
     * <p>{@code GET /api/v1/admin/articles}
     *
     * @param pageNum        页码，默认 1
     * @param pageSize       每页条数，默认 20，最大 50
     * @param workflowStatus 可选状态过滤：PENDING_REVIEW / PUBLISHED / REJECTED / UNPUBLISHED
     * @return 文章分页；分页参数或状态非法时返回 422
     */
    @GetMapping
    public ApiResponse<PageResponse<Article>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String workflowStatus) {
        return ApiResponse.success(service.list(pageNum, pageSize, workflowStatus), ApiTraceIdFilter.current());
    }

    /**
     * 某文章的审核记录（分页，只读，最新在前）。
     *
     * <p>{@code GET /api/v1/admin/articles/{id}/audit}
     *
     * @param id       文章 ID
     * @param pageNum  页码，默认 1
     * @param pageSize 每页条数，默认 20
     * @return 审核记录分页；文章不存在返回 404
     */
    @GetMapping("/{id}/audit")
    public ApiResponse<PageResponse<ModerationAuditRow>> audit(
            @PathVariable int id,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(service.audit(id, pageNum, pageSize), ApiTraceIdFilter.current());
    }

    /**
     * 审核通过：PENDING_REVIEW → PUBLISHED。
     *
     * <p>{@code POST /api/v1/admin/articles/{id}/approve}
     *
     * @param id      文章 ID
     * @param request 审核请求；{@code revision} 必填，理由可选
     * @return 更新后的文章；revision 过期或当前状态不允许时返回 409
     */
    @PostMapping("/{id}/approve")
    public ApiResponse<Article> approve(@PathVariable int id, @RequestBody @Valid ModerationRequest request) {
        return ApiResponse.success(service.approve(id, request), ApiTraceIdFilter.current());
    }

    /**
     * 审核拒绝：PENDING_REVIEW → REJECTED。
     *
     * <p>{@code POST /api/v1/admin/articles/{id}/reject}
     *
     * @param id      文章 ID
     * @param request 审核请求；{@code revision} 必填，理由可选
     * @return 更新后的文章；revision 过期或当前状态不允许时返回 409
     */
    @PostMapping("/{id}/reject")
    public ApiResponse<Article> reject(@PathVariable int id, @RequestBody @Valid ModerationRequest request) {
        return ApiResponse.success(service.reject(id, request), ApiTraceIdFilter.current());
    }

    /**
     * 下架：PUBLISHED → UNPUBLISHED。
     *
     * <p>{@code POST /api/v1/admin/articles/{id}/unpublish}
     *
     * @param id      文章 ID
     * @param request 审核请求；{@code revision} 必填，理由可选
     * @return 更新后的文章；revision 过期或当前状态不允许时返回 409
     */
    @PostMapping("/{id}/unpublish")
    public ApiResponse<Article> unpublish(@PathVariable int id, @RequestBody @Valid ModerationRequest request) {
        return ApiResponse.success(service.unpublish(id, request), ApiTraceIdFilter.current());
    }
}

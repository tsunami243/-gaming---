package org.example.bigevent.controller;

import jakarta.validation.Valid;
import org.example.bigevent.filter.ApiTraceIdFilter;
import org.example.bigevent.pojo.dto.AdminIngestRunRow;
import org.example.bigevent.pojo.dto.AdminSourceRow;
import org.example.bigevent.pojo.dto.ApiResponse;
import org.example.bigevent.pojo.dto.SourcePauseRequest;
import org.example.bigevent.service.AdminOperationsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端 · 采集来源与运行状态（v1）。
 *
 * <p>基础路径 {@code /api/v1/admin}；需 CONTENT_ADMIN / SUPER_ADMIN 角色。
 * 仅返回状态与计数，不返回任何密钥。
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminOperationsV1Controller {

    private final AdminOperationsService service;

    /**
     * 构造控制器。
     *
     * @param service 运营后台服务
     */
    public AdminOperationsV1Controller(AdminOperationsService service) {
        this.service = service;
    }

    /**
     * 来源状态列表。
     *
     * <p>{@code GET /api/v1/admin/sources}
     *
     * @return 全部内容来源（含 verificationStatus、autoPublishEnabled、暂停原因、最后成功时间）
     */
    @GetMapping("/sources")
    public ApiResponse<List<AdminSourceRow>> sources() {
        return ApiResponse.success(service.sources(), ApiTraceIdFilter.current());
    }

    /**
     * 暂停某来源：停止其后续采集，并关闭自动发布。
     *
     * <p>{@code POST /api/v1/admin/sources/{code}/pause}
     *
     * @param code    来源代码
     * @param request 暂停请求；{@code reason} 必填
     * @return 空数据；来源不存在或已暂停返回 409
     */
    @PostMapping("/sources/{code}/pause")
    public ApiResponse<Void> pause(@PathVariable String code, @RequestBody @Valid SourcePauseRequest request) {
        service.pause(code, request);
        return ApiResponse.success(null, ApiTraceIdFilter.current());
    }

    /**
     * 恢复某来源。
     *
     * <p>{@code POST /api/v1/admin/sources/{code}/resume}
     *
     * @param code 来源代码
     * @return 空数据；来源不存在或当前未暂停返回 409
     */
    @PostMapping("/sources/{code}/resume")
    public ApiResponse<Void> resume(@PathVariable String code) {
        service.resume(code);
        return ApiResponse.success(null, ApiTraceIdFilter.current());
    }

    /**
     * 最近采集批次。
     *
     * <p>{@code GET /api/v1/admin/ingest-runs}
     *
     * @param limit 返回条数，默认 20，允许 1~100
     * @return 最近的采集运行记录；limit 越界返回 422
     */
    @GetMapping("/ingest-runs")
    public ApiResponse<List<AdminIngestRunRow>> runs(@RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.success(service.latestRuns(limit), ApiTraceIdFilter.current());
    }
}

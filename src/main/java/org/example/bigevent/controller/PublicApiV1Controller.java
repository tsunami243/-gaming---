package org.example.bigevent.controller;

import org.example.bigevent.filter.ApiTraceIdFilter;
import org.example.bigevent.pojo.dto.ApiResponse;
import org.example.bigevent.pojo.dto.ArticleDetailResponse;
import org.example.bigevent.pojo.dto.ArticleSummaryResponse;
import org.example.bigevent.pojo.dto.GameResponse;
import org.example.bigevent.pojo.dto.PageResponse;
import org.example.bigevent.service.PublicArticleReadService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 公开端 · 资讯读取（v1，只读）。
 *
 * <p>基础路径 {@code /api/v1}，无需登录；只返回已发布（PUBLISHED）文章，
 * 公开响应一律使用 DTO 而非实体。
 *
 * <p>本控制器只负责接收请求参数与包装统一响应封套，全部业务规则
 * （参数校验、白名单、DTO 组装、正文读取）都在 {@link PublicArticleReadService}。
 */
@RestController
@RequestMapping("/api/v1")
public class PublicApiV1Controller {

    private final PublicArticleReadService readService;

    /**
     * 构造控制器。
     *
     * @param readService 公开读取服务（承载全部校验与 DTO 组装）
     */
    public PublicApiV1Controller(PublicArticleReadService readService) {
        this.readService = readService;
    }

    /**
     * 游戏字典（仅启用中的四游戏）。
     *
     * <p>{@code GET /api/v1/games}
     *
     * @return 游戏列表（code / 中英文名 / 别名 / 是否启用）
     */
    @GetMapping("/games")
    public ApiResponse<List<GameResponse>> games() {
        return ApiResponse.success(readService.listGames(), ApiTraceIdFilter.current());
    }

    /**
     * 已发布文章列表（分页，可按游戏与内容类型筛选）。
     *
     * <p>{@code GET /api/v1/articles}
     *
     * @param pageNum     页码，默认 1
     * @param pageSize    每页条数，默认 20，最大 50
     * @param gameCode    可选游戏代码（须在白名单内）
     * @param contentType 可选内容类型（须在六类内）
     * @return 文章摘要分页；gameCode / contentType / 分页参数非法时返回 422
     */
    @GetMapping("/articles")
    public ApiResponse<PageResponse<ArticleSummaryResponse>> articles(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String gameCode,
            @RequestParam(required = false) String contentType) {
        return ApiResponse.success(readService.listArticles(pageNum, pageSize, gameCode, contentType),
                ApiTraceIdFilter.current());
    }

    /**
     * 已发布文章详情，包含站内可读正文。
     *
     * <p>{@code GET /api/v1/articles/{id}}；正文按 id 从已发布记录读取，
     * FULL_TEXT 模式返回正文纯文本，其他模式 content 为 null。
     *
     * @param id 文章 ID
     * @return 文章详情；未发布或不存在返回 404
     */
    @GetMapping("/articles/{id}")
    public ApiResponse<ArticleDetailResponse> article(@PathVariable int id) {
        return ApiResponse.success(readService.articleDetail(id), ApiTraceIdFilter.current());
    }
}

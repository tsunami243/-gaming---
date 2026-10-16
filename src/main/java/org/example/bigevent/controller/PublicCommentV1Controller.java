package org.example.bigevent.controller;

import org.example.bigevent.filter.ApiTraceIdFilter;
import org.example.bigevent.mapper.PublicCommentMapper;
import org.example.bigevent.pojo.dto.ApiResponse;
import org.example.bigevent.pojo.dto.PageResponse;
import org.example.bigevent.pojo.dto.PublicCommentRow;
import org.example.bigevent.service.ArticleService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * 公开端 · 文章评论读取（v1，只读）。
 *
 * <p>基础路径 {@code /api/v1/articles/{id}/comments}，无需登录。
 * 仅返回已发布文章的可见评论；评论提交口不在本控制器内（当前服务端关闭）。
 */
@RestController
@RequestMapping("/api/v1/articles/{id}/comments")
public class PublicCommentV1Controller {

    private final PublicCommentMapper mapper;
    private final ArticleService articles;

    /**
     * 构造控制器。
     *
     * @param mapper   公开评论读取
     * @param articles 文章服务（校验文章已发布）
     */
    public PublicCommentV1Controller(PublicCommentMapper mapper, ArticleService articles) {
        this.mapper = mapper;
        this.articles = articles;
    }

    /**
     * 某篇已发布文章的公开评论（分页）。
     *
     * <p>{@code GET /api/v1/articles/{id}/comments}
     *
     * @param id       文章 ID
     * @param pageNum  页码，默认 1
     * @param pageSize 每页条数，默认 20，最大 50
     * @return 评论分页；分页参数非法返回 422；文章未发布或不存在返回 404
     */
    @GetMapping
    public ApiResponse<PageResponse<PublicCommentRow>> list(
            @PathVariable int id,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        if (id < 1 || pageNum < 1 || pageSize < 1 || pageSize > 50) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "分页参数无效");
        }
        articles.requirePublished(id);
        return ApiResponse.success(
                new PageResponse<>(mapper.page(id, pageSize, (long) (pageNum - 1) * pageSize), mapper.count(id), pageNum, pageSize),
                ApiTraceIdFilter.current());
    }
}

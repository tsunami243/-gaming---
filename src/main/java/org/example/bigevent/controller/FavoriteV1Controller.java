package org.example.bigevent.controller;

import org.example.bigevent.filter.ApiTraceIdFilter;
import org.example.bigevent.mapper.FavoriteReadMapper;
import org.example.bigevent.pojo.dto.ApiResponse;
import org.example.bigevent.pojo.dto.FavoriteSummary;
import org.example.bigevent.pojo.dto.PageResponse;
import org.example.bigevent.utils.ThreadLocalUtil;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * 本人 · 收藏列表（v1）。
 *
 * <p>基础路径 {@code /api/v1/me/favorites}，需登录。
 * 用户身份只取自 JWT claims（{@code userId}），不接受请求参数传入，避免越权读取他人收藏。
 */
@RestController
@RequestMapping("/api/v1/me/favorites")
public class FavoriteV1Controller {

    private final FavoriteReadMapper mapper;

    /**
     * 构造控制器。
     *
     * @param mapper 收藏读取
     */
    public FavoriteV1Controller(FavoriteReadMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 当前登录用户的收藏（分页，仅已发布文章）。
     *
     * <p>{@code GET /api/v1/me/favorites}
     *
     * @param pageNum  页码，默认 1
     * @param pageSize 每页条数，默认 20，最大 50
     * @return 收藏分页；无有效登录凭据返回 401；分页参数非法返回 422
     */
    @GetMapping
    public ApiResponse<PageResponse<FavoriteSummary>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        if (claims == null || !(claims.get("userId") instanceof Integer userId) || userId < 1) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请重新登录");
        }
        if (pageNum < 1 || pageSize < 1 || pageSize > 50) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "分页参数无效");
        }
        return ApiResponse.success(new PageResponse<>(mapper.page(userId, pageSize, (long) (pageNum - 1) * pageSize),
                mapper.count(userId), pageNum, pageSize), ApiTraceIdFilter.current());
    }
}

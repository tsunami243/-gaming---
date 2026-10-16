package org.example.bigevent.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.bigevent.pojo.Result;
import org.example.bigevent.service.ArticleService;
import org.example.bigevent.service.ViewHistoryService;
import org.example.bigevent.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 文章浏览记录（legacy 接口）。
 *
 * <p>基础路径 {@code /article-view}，无需登录（游客按 IP 去重，登录用户按 userId，窗口 24 小时）。
 * 目标文章必须已发布。注意：记录浏览的入口当前前端未调用，故浏览量/热度在 UI 层尚未生效。
 */
@RestController
@RequestMapping("/article-view")
public class ViewHistoryController {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private ViewHistoryService viewHistoryService;

    /**
     * 记录一次浏览（通常由文章详情页自动调用）。
     *
     * <p>{@code POST /article-view?articleId=}
     *
     * @param articleId 文章 ID（须已发布）
     * @param request   用于取客户端 IP
     * @return 空数据；文章未发布返回 404
     */
    @PostMapping
    public Result recordView(@RequestParam Integer articleId, HttpServletRequest request) {
        articleService.requirePublished(articleId);
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (map != null) ? (Integer) map.get("userId") : null;
        String ipAddress = getClientIp(request);
        viewHistoryService.recordView(articleId, userId, ipAddress);
        return Result.success();
    }

    /**
     * 文章总浏览数。
     *
     * <p>{@code GET /article-view/count?articleId=}
     *
     * @param articleId 文章 ID（须已发布）
     * @return 浏览量
     */
    @GetMapping("/count")
    public Result<Integer> getViewCount(@RequestParam Integer articleId) {
        articleService.requirePublished(articleId);
        Integer count = viewHistoryService.getViewCount(articleId);
        return Result.success(count);
    }

    /**
     * 文章独立浏览数（24 小时去重）。
     *
     * <p>{@code GET /article-view/unique-count?articleId=}
     *
     * @param articleId 文章 ID（须已发布）
     * @return 独立浏览量
     */
    @GetMapping("/unique-count")
    public Result<Integer> getUniqueViewCount(@RequestParam Integer articleId) {
        articleService.requirePublished(articleId);
        Integer count = viewHistoryService.getUniqueViewCount(articleId);
        return Result.success(count);
    }

    /** 依次从常见反向代理头中解析客户端 IP，多级代理取第一个。 */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理时，取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}

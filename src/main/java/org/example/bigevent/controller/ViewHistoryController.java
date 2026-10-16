package org.example.bigevent.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.bigevent.pojo.Result;
import org.example.bigevent.service.ArticleService;
import org.example.bigevent.service.ViewHistoryService;
import org.example.bigevent.utils.ClientIpUtil;
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
 * <p>基础路径 {@code /article-view}，无需登录（游客按 IP 去重，登录用户按 userId）。
 * 目标文章必须已发布。注意：记录浏览的入口当前前端未调用，故浏览量/热度在 UI 层尚未生效。</p>
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
     * 文章独立浏览数。
     *
     * <p>{@code GET /article-view/unique-count?articleId=}</p>
     *
     * <p><b>口径说明</b>：数据库侧是<b>全时段</b>按 {@code user_id} 去重，且
     * {@code user_id is null} 的游客记录不计入——并非上文 24 小时窗口，
     * 也不是按 IP 去重的口径。</p>
     *
     * @param articleId 文章 ID（须已发布）
     * @return 独立浏览量（去重后的登录用户数）
     */
    @GetMapping("/unique-count")
    public Result<Integer> getUniqueViewCount(@RequestParam Integer articleId) {
        articleService.requirePublished(articleId);
        Integer count = viewHistoryService.getUniqueViewCount(articleId);
        return Result.success(count);
    }

    /**
     * 解析客户端 IP，用于去重键。
     *
     * <p>代理头可伪造，因此只采信校验通过的 IP 字面量，否则回退 {@code remoteAddr}——
     * 详见 {@link ClientIpUtil} 与 {@code deploy/nginx/default.conf} 的覆盖语义。</p>
     *
     * @param request 当前请求
     * @return 可信的客户端 IP
     */
    private String getClientIp(HttpServletRequest request) {
        return ClientIpUtil.resolve(request);
    }
}

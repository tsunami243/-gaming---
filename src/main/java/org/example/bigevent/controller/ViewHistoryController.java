package org.example.bigevent.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.bigevent.pojo.Result;
import org.example.bigevent.service.ViewHistoryService;
import org.example.bigevent.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/article-view")
public class ViewHistoryController {
    @Autowired
    private ViewHistoryService viewHistoryService;

    // 记录浏览（内部接口，通常由文章详情页自动调用）
    @PostMapping
    public Result recordView(@RequestParam Integer articleId, HttpServletRequest request) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("userId");
        String ipAddress = getClientIp(request);
        viewHistoryService.recordView(articleId, userId, ipAddress);
        return Result.success();
    }

    // 获取浏览数
    @GetMapping("/count")
    public Result<Integer> getViewCount(@RequestParam Integer articleId) {
        Integer count = viewHistoryService.getViewCount(articleId);
        return Result.success(count);
    }

    // 获取独立浏览数
    @GetMapping("/unique-count")
    public Result<Integer> getUniqueViewCount(@RequestParam Integer articleId) {
        Integer count = viewHistoryService.getUniqueViewCount(articleId);
        return Result.success(count);
    }

    // 获取客户端IP地址
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

package org.example.bigevent.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 链路追踪 ID 过滤器：只对新的 v1 接口面生效。
 *
 * <p>优先复用上游传入的 {@code X-Trace-Id}（长度上限 128，超长视为非法），
 * 否则生成新的 UUID；随后回写响应头，并在请求结束时清理 ThreadLocal，避免串号。</p>
 */
@Component
public class ApiTraceIdFilter extends OncePerRequestFilter {

    /** 追踪 ID 的请求/响应头名。 */
    public static final String HEADER = "X-Trace-Id";

    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();

    /**
     * 仅拦截 {@code /api/v1/} 前缀的请求，其余（legacy 接口、静态资源）直接放行。
     *
     * @param request 当前请求
     * @return 不需要处理时返回 {@code true}
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/v1/");
    }

    /**
     * 解析或生成追踪 ID，写入响应头并绑定到当前线程。
     *
     * @param request     当前请求
     * @param response    当前响应
     * @param filterChain 过滤器链
     * @throws ServletException 下游过滤器抛出时透传
     * @throws IOException      下游读写异常时透传
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = request.getHeader(HEADER);
        if (traceId == null || traceId.isBlank() || traceId.length() > 128) {
            traceId = UUID.randomUUID().toString();
        }
        TRACE_ID.set(traceId);
        response.setHeader(HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            TRACE_ID.remove();
        }
    }

    /**
     * 读取当前请求的追踪 ID，供响应封套使用。
     *
     * @return 当前线程绑定的追踪 ID；不在 v1 请求上下文中时返回一个新生成的 UUID
     */
    public static String current() {
        String value = TRACE_ID.get();
        return value == null ? UUID.randomUUID().toString() : value;
    }
}

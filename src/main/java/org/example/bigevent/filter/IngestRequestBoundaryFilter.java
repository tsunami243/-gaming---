package org.example.bigevent.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 内部采集接口的边界过滤器：在 JSON 反序列化之前完成鉴权与请求体约束。
 *
 * <p>处理顺序（任一不通过即直接返回错误响应，不再进入业务链路）：</p>
 * <ol>
 *   <li>校验 {@code X-Ingest-Service-Token}（常量时间比较，防时序侧信道攻击）</li>
 *   <li>拒绝带 {@code Content-Encoding} 的压缩请求体（防解压炸弹）</li>
 *   <li>按 {@code Content-Length} 与实读字节双重限制请求体大小</li>
 *   <li>把请求体读进内存并包装成可重复读取的流，供下游控制器消费</li>
 * </ol>
 *
 * <p>fail-closed：未配置令牌（配置为空）时一律拒绝，不会退化为"放行所有人"。</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class IngestRequestBoundaryFilter extends OncePerRequestFilter {

    /** 采集请求体允许的最大字节数。 */
    static final int MAX_BYTES = 256_000;

    private final byte[] token;

    /**
     * 构造过滤器。
     *
     * @param token 采集服务令牌，空串表示未配置（此时所有采集请求都会被拒绝）
     */
    public IngestRequestBoundaryFilter(@Value("${ingest.service-token:}") String token) {
        this.token = token.isBlank() ? new byte[0] : token.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 仅拦截 {@code /internal/v1/ingest/} 前缀的请求，其余直接放行。
     *
     * <p>先去掉 contextPath 再判断，兼容部署在非根路径的场景。</p>
     *
     * @param request 当前请求
     * @return 不需要处理时返回 {@code true}
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().substring(request.getContextPath().length()).startsWith("/internal/v1/ingest/");
    }

    /**
     * 执行鉴权与请求体边界检查，通过后把请求交给下游。
     *
     * @param request 当前请求
     * @param response 当前响应
     * @param chain    过滤器链
     * @throws IOException      读取请求体失败时抛出
     * @throws ServletException 下游过滤器抛出时透传
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!authenticated(request)) {
            fail(response, 403, "invalid ingest service identity");
            return;
        }
        if (!"POST".equals(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }
        String encoding = request.getHeader("Content-Encoding");
        if (encoding != null && !"identity".equalsIgnoreCase(encoding)) {
            fail(response, 415, "compressed ingest payloads are unsupported");
            return;
        }
        if (request.getContentLengthLong() > MAX_BYTES) {
            fail(response, 413, "ingest payload exceeds byte limit");
            return;
        }

        // 多读 1 字节用于识别"声明长度合规但实读超限"的情况
        byte[] body = request.getInputStream().readNBytes(MAX_BYTES + 1);
        if (body.length > MAX_BYTES) {
            fail(response, 413, "ingest payload exceeds byte limit");
            return;
        }
        chain.doFilter(new BufferedBodyRequest(request, body), response);
    }

    /**
     * 校验采集服务令牌（常量时间比较）。
     *
     * @param request 当前请求
     * @return 令牌已配置且与请求头一致时返回 {@code true}
     */
    private boolean authenticated(HttpServletRequest request) {
        String supplied = request.getHeader("X-Ingest-Service-Token");
        if (token.length == 0 || supplied == null) {
            return false;
        }
        return MessageDigest.isEqual(token, supplied.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 输出统一的错误响应体并结束处理。
     *
     * @param response 当前响应
     * @param status   HTTP 状态码
     * @param message  错误提示
     * @throws IOException 写响应失败时抛出
     */
    private static void fail(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"code\":1,\"message\":\"" + message + "\",\"data\":null}");
    }

    /**
     * 把已读入内存的请求体包装成可重复读取的请求，避免下游再次读流时拿不到数据。
     */
    private static final class BufferedBodyRequest extends HttpServletRequestWrapper {

        private final byte[] body;

        /**
         * 构造包装请求。
         *
         * @param request 原始请求
         * @param body    已读出的请求体字节
         */
        private BufferedBodyRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        /**
         * 返回基于内存字节的输入流。
         *
         * @return 可重复读取的输入流
         */
        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream input = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public int read() {
                    return input.read();
                }

                @Override
                public int read(byte[] b, int off, int len) {
                    return input.read(b, off, len);
                }

                @Override
                public boolean isFinished() {
                    return input.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener listener) {
                    throw new UnsupportedOperationException("synchronous ingest only");
                }
            };
        }

        /**
         * 返回请求体的字符流。
         *
         * @return UTF-8 编码的 Reader
         */
        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}

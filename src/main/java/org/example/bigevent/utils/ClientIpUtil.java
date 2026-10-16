package org.example.bigevent.utils;

import jakarta.servlet.http.HttpServletRequest;

import java.util.regex.Pattern;

/**
 * 可信客户端 IP 解析。
 *
 * <p>代理头（{@code X-Forwarded-For} 等）本质是客户端可自由伪造的普通请求头，
 * 不能无条件采信。这里的规则是：只接受<b>能解析为 IP 字面量</b>的值（IPv4 / IPv6），
 * 其余一律忽略并回退到 {@link HttpServletRequest#getRemoteAddr()}。</p>
 *
 * <p>关于取值位置：本项目 nginx 用 {@code proxy_set_header X-Forwarded-For $remote_addr;}
 * <b>覆盖</b>该头（见 {@code deploy/nginx/default.conf}），因此链上只有一跳。若将来在
 * nginx 前再加一层代理且改成追加语义，则此处应改为「从右往左数第 N+1 段」，
 * 而不是简单取第一段——取第一段等于采信了攻击者自己填的值。</p>
 */
public final class ClientIpUtil {

    /** 直连（无代理）时无法伪造的兜底值。 */
    private static final String UNKNOWN = "unknown";

    /** IPv4 字面量（每段 0~255）。 */
    private static final Pattern IPV4 = Pattern.compile(
            "^(?:(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)$");

    /** IPv6 字面量（至少两段十六进制，允许 {@code ::} 压缩写法）。 */
    private static final Pattern IPV6 = Pattern.compile(
            "^(?:[0-9A-Fa-f]{0,4}:){2,7}[0-9A-Fa-f]{0,4}$");

    private ClientIpUtil() {
    }

    /**
     * 解析请求的可信客户端 IP。
     *
     * @param request 当前请求
     * @return 校验通过的代理头值；缺失或非法时返回 {@code remoteAddr}；
     *         {@code remoteAddr} 也为空时返回 {@link #UNKNOWN}
     */
    public static String resolve(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // 链上多跳时取最后一段：它由我们自己的代理写入，客户端无法伪造。
            String[] hops = forwarded.split(",");
            String candidate = hops[hops.length - 1].trim();
            if (isIpLiteral(candidate)) {
                return candidate;
            }
        }
        String remote = request.getRemoteAddr();
        return (remote == null || remote.isBlank()) ? UNKNOWN : remote;
    }

    /**
     * 判断字符串是否为 IP 字面量。
     *
     * @param value 待判定值
     * @return 形如 IPv4 或 IPv6 时返回 {@code true}
     */
    public static boolean isIpLiteral(String value) {
        return value != null && (IPV4.matcher(value).matches() || IPV6.matcher(value).matches());
    }
}

package org.example.bigevent.pojo.dto;

/** v1 错误响应体：业务码 + 提示信息 + 可选数据 + 追踪 ID（traceId 便于串联日志）。 */
public record ApiErrorResponse(int code, String message, Object data, String traceId) {

    /**
     * 构造错误响应。
     *
     * @param message 错误提示
     * @param traceId 追踪 ID
     * @return code=1、data 为 null 的错误封套
     */
    public static ApiErrorResponse error(String message, String traceId) {
        return new ApiErrorResponse(1, message, null, traceId);
    }
}

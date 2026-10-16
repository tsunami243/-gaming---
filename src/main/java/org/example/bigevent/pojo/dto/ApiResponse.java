package org.example.bigevent.pojo.dto;

/**
 * v1 成功响应封套。
 *
 * <p>legacy 的 {@code Result} 仅保留给尚未迁移的旧接口，新接口统一使用本类型。</p>
 *
 * @param code    业务状态码，0 表示成功
 * @param message 提示信息
 * @param data    业务数据
 * @param traceId 追踪 ID，便于串联日志
 * @param <T>     业务数据类型
 */
public record ApiResponse<T>(int code, String message, T data, String traceId) {

    /**
     * 构造成功响应。
     *
     * @param data    业务数据
     * @param traceId 追踪 ID
     * @param <T>     业务数据类型
     * @return code=0、message=OK 的响应封套
     */
    public static <T> ApiResponse<T> success(T data, String traceId) {
        return new ApiResponse<>(0, "OK", data, traceId);
    }
}

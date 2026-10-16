package org.example.bigevent.exception;

import org.example.bigevent.controller.AdminArticleV1Controller;
import org.example.bigevent.controller.AdminOperationsV1Controller;
import org.example.bigevent.controller.FavoriteV1Controller;
import org.example.bigevent.controller.PublicApiV1Controller;
import org.example.bigevent.controller.PublicCommentV1Controller;
import org.example.bigevent.filter.ApiTraceIdFilter;
import org.example.bigevent.pojo.dto.ApiErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

/**
 * v1 接口的异常处理器：保证失败响应与成功响应使用同一套封套结构。
 *
 * <p>结构整理后 v1 与 legacy/内部控制器同处 controller 包，无法再用 {@code basePackages}
 * 区分，故显式列出 v1 控制器类型，保持原有错误封套范围与状态码不变。</p>
 *
 * <p>注意：{@code InternalIngestController} 有意不在名单内——它的错误必须是 400 而非 422，
 * 改动此处会破坏内部采集接口的契约。</p>
 */
@RestControllerAdvice(assignableTypes = {
        PublicApiV1Controller.class, PublicCommentV1Controller.class, FavoriteV1Controller.class,
        AdminArticleV1Controller.class, AdminOperationsV1Controller.class})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiV1ExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApiV1ExceptionHandler.class);

    /**
     * 处理请求体不可解析、必填参数缺失、参数类型不匹配三类客户端错误。
     *
     * @param exception 原始异常（仅用于日志与类型匹配，不向外暴露细节）
     * @return 400 错误封套
     */
    @ExceptionHandler({HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiErrorResponse> badRequest(Exception exception) {
        return ResponseEntity.badRequest().body(ApiErrorResponse.error("请求格式或参数无效", ApiTraceIdFilter.current()));
    }

    /**
     * 兜底处理未被其它分支捕获的异常，避免把堆栈细节泄露给客户端。
     *
     * @param exception 原始异常
     * @return 500 错误封套
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> unexpected(Exception exception) {
        LOG.error("Unhandled v1 failure traceId={} type={}", ApiTraceIdFilter.current(), exception.getClass().getSimpleName());
        return ResponseEntity.internalServerError().body(ApiErrorResponse.error("服务暂不可用，请稍后重试", ApiTraceIdFilter.current()));
    }

    /**
     * 处理 Bean Validation 校验失败（{@code @Valid} 注解触发）。
     *
     * @param exception 校验异常
     * @return 422 错误封套
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> validation(MethodArgumentNotValidException exception) {
        return ResponseEntity.unprocessableEntity()
                .body(ApiErrorResponse.error("请求字段校验失败", ApiTraceIdFilter.current()));
    }

    /**
     * 处理业务层主动抛出的 {@link ResponseStatusException}，透传其状态码与原因。
     *
     * @param exception 业务异常
     * @return 状态码取自异常本身的错误封套
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> responseStatus(ResponseStatusException exception) {
        String message = StringUtils.hasLength(exception.getReason()) ? exception.getReason() : "请求失败";
        return ResponseEntity.status(exception.getStatusCode())
                .body(ApiErrorResponse.error(message, ApiTraceIdFilter.current()));
    }
}

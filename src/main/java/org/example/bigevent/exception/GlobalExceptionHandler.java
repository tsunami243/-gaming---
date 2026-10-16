package org.example.bigevent.exception;

import org.example.bigevent.pojo.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

/**
 * legacy 接口的全局异常处理器：把异常统一转换成 {@link Result} 封套。
 *
 * <p>与 {@code ApiV1ExceptionHandler} 的分工：本类覆盖所有控制器（无 assignableTypes 限定），
 * 而 v1 处理器通过 {@code @Order(HIGHEST_PRECEDENCE)} 优先命中 v1 控制器。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理请求体不可解析、必填参数/请求头缺失、参数类型不匹配四类客户端错误。
     *
     * @param exception 原始异常
     * @return 400 + 统一失败封套
     */
    @ExceptionHandler({HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MissingRequestHeaderException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Result<Void>> badRequest(Exception exception) {
        return ResponseEntity.badRequest().body(Result.error("请求格式或参数无效"));
    }

    /**
     * 处理业务层主动抛出的 {@link ResponseStatusException}，透传其状态码与原因。
     *
     * @param exception 业务异常
     * @return 状态码取自异常本身的失败封套
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Result<Void>> handleResponseStatusException(ResponseStatusException exception) {
        String message = StringUtils.hasLength(exception.getReason()) ? exception.getReason() : "操作失败";
        return ResponseEntity.status(exception.getStatusCode()).body(Result.error(message));
    }

    /**
     * 处理 Bean Validation 校验失败，把首个字段错误拼进提示信息。
     *
     * @param exception 校验异常
     * @return 400 + 失败封套
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream().findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage()).orElse("请求参数错误");
        return ResponseEntity.badRequest().body(Result.error(message));
    }

    /**
     * 兜底处理未被其它分支捕获的异常，避免把堆栈细节泄露给客户端。
     *
     * @param exception 原始异常
     * @return 500 + 失败封套
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<String>> handleException(Exception exception) {
        LOG.error("Unhandled request failure type={}", exception.getClass().getSimpleName());
        return ResponseEntity.status(500).body(Result.error("服务暂不可用，请稍后重试"));
    }
}

package com.railway.common.exception;

import com.railway.common.model.R;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.stream.Collectors;

/**
 * 全局异常处理：把异常统一转 R 返回给前端。
 *
 * <p>处理：
 * <ul>
 *   <li>{@link BizException} - 业务异常</li>
 *   <li>{@link MethodArgumentNotValidException} / {@link BindException} - @Valid 失败</li>
 *   <li>{@link ConstraintViolationException} - 单参数校验失败</li>
 *   <li>{@link MissingServletRequestParameterException} - 缺参数</li>
 *   <li>{@link HttpRequestMethodNotSupportedException} - 请求方法不允许</li>
 *   <li>{@link HttpMessageNotReadableException} - body 解析失败</li>
 *   <li>{@link Exception} - 兜底 500（不暴露堆栈）</li>
 * </ul>
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    @ResponseBody
    public R<Void> handleBizException(BizException e, HttpServletRequest request) {
        log.warn("[BizException] {} {} -> code={} msg={}",
                request.getMethod(), request.getRequestURI(), e.getCode(), e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public R<Void> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        log.warn("[Validation] {}", msg);
        return R.fail(ErrorCode.BAD_REQUEST.getCode(), msg);
    }

    @ExceptionHandler(BindException.class)
    @ResponseBody
    public R<Void> handleBind(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        log.warn("[Bind] {}", msg);
        return R.fail(ErrorCode.BAD_REQUEST.getCode(), msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseBody
    public R<Void> handleConstraint(ConstraintViolationException e) {
        log.warn("[Constraint] {}", e.getMessage());
        return R.fail(ErrorCode.BAD_REQUEST.getCode(), e.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseBody
    public R<Void> handleMissingParam(MissingServletRequestParameterException e) {
        return R.fail(ErrorCode.BAD_REQUEST.getCode(), "缺少参数: " + e.getParameterName());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseBody
    public R<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return R.fail(ErrorCode.METHOD_NOT_ALLOWED.getCode(),
                "不支持的请求方法: " + e.getMethod());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseBody
    public R<Void> handleMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("[MessageNotReadable] {}", e.getMessage());
        return R.fail(ErrorCode.BAD_REQUEST.getCode(), "请求体格式错误");
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public R<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("[UnhandledException] {} {}", request.getMethod(), request.getRequestURI(), e);
        return R.fail(ErrorCode.SERVER_ERROR);
    }

    private String formatFieldError(FieldError fe) {
        return fe.getField() + ": " + fe.getDefaultMessage();
    }
}

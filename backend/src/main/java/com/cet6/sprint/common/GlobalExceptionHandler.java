package com.cet6.sprint.common;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理
 * <p>
 * 把散落在各处的 try-catch 收敛到一处，Controller 只管写业务。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：属于预期内的错误，记 warn 即可 */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /** 参数校验失败（@Valid + @RequestBody） */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("参数校验失败");
        return Result.fail(400, msg);
    }

    /** 参数绑定失败（表单/查询参数） */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBind(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("参数绑定失败");
        return Result.fail(400, msg);
    }

    /**
     * 参数校验失败（方法级 @Validated + @RequestParam / @PathVariable）
     * <p>
     * 与 {@link MethodArgumentNotValidException} 的区别：后者针对 @RequestBody，
     * 本异常针对 URL 上的查询参数/路径变量。若不单独处理会落到兜底分支返回 500，
     * 把「参数不合法」误报成「服务器错误」。
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraint(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .findFirst()
                .map(ConstraintViolation::getMessage)
                .orElse("参数校验失败");
        return Result.fail(400, msg);
    }

    /**
     * 请求体不可解析（JSON 格式错误 / 请求体缺失）
     * <p>
     * 属于客户端问题，应返回 400 而不是 500，便于前端区分「我传错了」和「服务端挂了」。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleUnreadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return Result.fail(400, "请求参数格式不正确");
    }

    /** 兜底：未预期的异常，打印完整堆栈便于排查 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception e) {
        log.error("系统异常", e);
        return Result.fail(500, "服务器开小差了，请稍后再试");
    }
}

package common.config;

import common.exception.BaseCodeEnum;
import common.exception.BaseException;
import common.exception.ValidationException;
import common.utils.R;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 全局异常出口，把各类异常压平成统一的 {@code R}。
 *
 * <p>字段级错误的明细放 {@code data}，和其它接口一个形状，调用方只认一种响应体。</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常，code 由抛出方指定 */
    @ExceptionHandler(BaseException.class)
    public R<Void> handleException(BaseException e) {
        return R.error(e.getCode(), e.getMessage());
    }

    /** 手写校验失败，产出和下面几条注解路径完全一样 */
    @ExceptionHandler(ValidationException.class)
    public R<Map<String, String>> handleManualValidation(ValidationException e) {
        return R.error(e.getCode(), e.getMessage(), e.getErrors());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Map<String, String>> handleValidationException(MethodArgumentNotValidException ex) {
        BindingResult bindingResult = ex.getBindingResult();
        // LinkedHashMap：errors 会被拼成一句话给用户看，顺序必须稳定
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            errors.put(fieldError.getField(), describeFieldError(fieldError));
        }
        return R.error(BaseCodeEnum.VALID_EXCEPTION, errors);
    }

    /** query 参数校验失败。给参数加约束需要类上标 {@code @Validated}，走的是这条 */
    @ExceptionHandler(ConstraintViolationException.class)
    public R<Map<String, String>> handleConstraintViolationException(ConstraintViolationException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            // propertyPath 形如 sendCode.mobile，只取最后一段当字段名
            String path = violation.getPropertyPath().toString();
            int dot = path.lastIndexOf('.');
            errors.put(dot < 0 ? path : path.substring(dot + 1), violation.getMessage());
        }
        return R.error(BaseCodeEnum.VALID_EXCEPTION, errors);
    }

    /** 必填 query 参数没传。不接的话 Spring 返回它自己那套没有 code/msg 的响应体 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public R<Map<String, String>> handleMissingParameter(MissingServletRequestParameterException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        errors.put(ex.getParameterName(), "不能为空");
        return R.error(BaseCodeEnum.VALID_EXCEPTION, errors);
    }

    /** query 参数类型不对，例如 {@code pageNum=abc} */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public R<Map<String, String>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        errors.put(ex.getName(), typeMismatchMessage(ex.getValue()));
        return R.error(BaseCodeEnum.VALID_EXCEPTION, errors);
    }

    /** 注解校验失败时注解上写的就是中文，直接用；类型转换失败是英文长句，换成短句 */
    private static String describeFieldError(FieldError fieldError) {
        return fieldError.isBindingFailure()
                ? typeMismatchMessage(fieldError.getRejectedValue())
                : fieldError.getDefaultMessage();
    }

    /** 原值截断后回显，否则一个超长参数就能把提示撑成一屏 */
    private static String typeMismatchMessage(Object rejectedValue) {
        if (rejectedValue == null) {
            return TYPE_MISMATCH_MESSAGE;
        }
        String text = String.valueOf(rejectedValue);
        if (text.length() > MAX_REJECTED_VALUE_LENGTH) {
            text = text.substring(0, MAX_REJECTED_VALUE_LENGTH) + "…";
        }
        return TYPE_MISMATCH_MESSAGE + "：" + text;
    }

    private static final String TYPE_MISMATCH_MESSAGE = "参数类型不正确";

    private static final int MAX_REJECTED_VALUE_LENGTH = 50;

    /** 请求体不是合法 JSON。真实异常文本含类名和字段路径，只进日志 */
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public R<Map<String, String>> handleHttpMessageNotReadableException(
            org.springframework.http.converter.HttpMessageNotReadableException ex) {
        log.error("HttpMessageNotReadableException:", ex);
        Map<String, String> errors = new LinkedHashMap<>();
        errors.put("body", "请求体不是合法的 JSON");
        return R.error(BaseCodeEnum.JSON_EXCEPTION, errors);
    }

    /** 兜底。未预期异常本来会走 Spring 默认的 {@code {timestamp,status,error,path}}，没有 code */
    @ExceptionHandler(Exception.class)
    public R<Void> handleUnexpectedException(Exception ex) {
        if (ex instanceof ErrorResponse errorResponse) {
            // 405/415 这类"调用方式不对"单独给码，和 10000 真异常分开，状态码同样压平成 200
            log.warn("请求不被接受: status={} {}", errorResponse.getStatusCode().value(), ex.getMessage());
            return R.error(BaseCodeEnum.REQUEST_NOT_ACCEPTABLE);
        }
        log.error("未预期异常:", ex);
        return R.error(BaseCodeEnum.UNKNOWN_EXCEPTION);
    }
}

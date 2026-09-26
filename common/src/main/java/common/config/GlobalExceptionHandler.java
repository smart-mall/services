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

    /**
     * 处理业务异常，code 与文案都由抛出方给出。
     *
     * @param e 业务异常
     * @return 携带抛出方指定 code 与 message 的失败响应
     */
    @ExceptionHandler(BaseException.class)
    public R<Void> handleException(BaseException e) {
        return R.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理手写的字段级校验失败，产出与注解校验那两条路径完全一致。
     *
     * @param e 携带字段明细的校验异常
     * @return code 为 {@link BaseCodeEnum#VALID_EXCEPTION}、data 为字段名到消息映射的失败响应
     */
    @ExceptionHandler(ValidationException.class)
    public R<Map<String, String>> handleManualValidation(ValidationException e) {
        return R.error(e.getCode(), e.getMessage(), e.getErrors());
    }

    /**
     * 处理 {@code @Valid} 触发的请求体字段校验失败。
     *
     * @param ex Spring 抛出的方法参数校验异常
     * @return code 为 {@link BaseCodeEnum#VALID_EXCEPTION}、data 为字段名到消息映射的失败响应
     */
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

    /**
     * 处理 query 参数校验失败。
     *
     * <p>给方法参数加约束需要类上标 {@code @Validated} 才会走到这条。
     *
     * @param ex 约束校验异常，可能包含多个字段的违规项
     * @return code 为 {@link BaseCodeEnum#VALID_EXCEPTION}、data 为字段名到消息映射的失败响应
     */
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

    /**
     * 处理必填 query 参数缺失。
     *
     * <p>不接的话 Spring 会返回它自己那套没有 code/msg 的响应体。
     *
     * @param ex 缺参异常
     * @return code 为 {@link BaseCodeEnum#VALID_EXCEPTION}、data 为参数名到"不能为空"的失败响应
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public R<Map<String, String>> handleMissingParameter(MissingServletRequestParameterException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        errors.put(ex.getParameterName(), "不能为空");
        return R.error(BaseCodeEnum.VALID_EXCEPTION, errors);
    }

    /**
     * 处理 query 参数类型不匹配，例如 {@code page=abc}。
     *
     * @param ex 类型转换失败异常
     * @return code 为 {@link BaseCodeEnum#VALID_EXCEPTION}、data 为参数名到提示的失败响应
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public R<Map<String, String>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        errors.put(ex.getName(), typeMismatchMessage(ex.getValue()));
        return R.error(BaseCodeEnum.VALID_EXCEPTION, errors);
    }

    /**
     * 取字段错误的提示文案：注解上写的就是中文，直接用；类型转换失败是英文长句，换成短句。
     *
     * @param fieldError 单个字段的错误
     * @return 可直接展示给用户的提示文案
     */
    private static String describeFieldError(FieldError fieldError) {
        return fieldError.isBindingFailure()
                ? typeMismatchMessage(fieldError.getRejectedValue())
                : fieldError.getDefaultMessage();
    }

    /**
     * 拼接类型不匹配的提示，原值截断后回显，否则一个超长参数就能把提示撑成一屏。
     *
     * @param rejectedValue 被拒绝的原值，允许为 {@code null}
     * @return 形如"参数类型不正确：abc"的提示；原值为空时只有前半句
     */
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

    /**
     * 处理请求体不是合法 JSON 的情况。
     *
     * <p>真实异常文本含类名和字段路径，只进日志，不回给调用方。
     *
     * @param ex 消息体反序列化失败异常
     * @return code 为 {@link BaseCodeEnum#JSON_EXCEPTION}、data 只有 {@code body} 一项的失败响应
     */
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public R<Map<String, String>> handleHttpMessageNotReadableException(
            org.springframework.http.converter.HttpMessageNotReadableException ex) {
        log.error("HttpMessageNotReadableException:", ex);
        Map<String, String> errors = new LinkedHashMap<>();
        errors.put("body", "请求体不是合法的 JSON");
        return R.error(BaseCodeEnum.JSON_EXCEPTION, errors);
    }

    /**
     * 兜底处理未被前面几条命中的异常。
     *
     * <p>未预期异常不接的话会走 Spring 默认的 {@code {timestamp,status,error,path}}，没有 code。
     *
     * @param ex 未预期异常
     * @return 调用方式不对时返回 {@link BaseCodeEnum#REQUEST_NOT_ACCEPTABLE}，
     *         其余返回 {@link BaseCodeEnum#UNKNOWN_EXCEPTION}
     */
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

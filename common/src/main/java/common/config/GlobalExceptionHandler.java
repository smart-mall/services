package common.config;

import common.exception.BaseCodeEnum;
import common.exception.BaseException;
import common.utils.R;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BaseException.class)
    public R handleException(BaseException e) {
        return R.error(444, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R handleValidationException(MethodArgumentNotValidException ex) {
        BindingResult bindingResult = ex.getBindingResult();
        Map<String, String> errors = new HashMap<>();

        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return R.error(BaseCodeEnum.VALID_EXCEPTION.getCode(),BaseCodeEnum.VALID_EXCEPTION.getMsg()).put("errors", errors);
    }

    /**
     * 处理 request param / path variable 上的校验失败。
     *
     * <p>和 {@link #handleValidationException} 是两套机制：请求体走 {@code @Valid} 抛
     * {@code MethodArgumentNotValidException}，而给 query 参数加约束需要类上标
     * {@code @Validated}，失败时抛的是 {@code ConstraintViolationException}。
     * 两者最终都拼成同一套 {@code code:10001 + errors{字段:消息}}，前端只认一个格式。</p>
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public R handleConstraintViolationException(ConstraintViolationException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            // propertyPath 形如 sendCode.mobile，只取最后一段当字段名
            String path = violation.getPropertyPath().toString();
            int dot = path.lastIndexOf('.');
            errors.put(dot < 0 ? path : path.substring(dot + 1), violation.getMessage());
        }
        return R.error(BaseCodeEnum.VALID_EXCEPTION.getCode(), BaseCodeEnum.VALID_EXCEPTION.getMsg()).put("errors", errors);
    }

    /**
     * 处理必填的 query 参数没传。
     *
     * <p>不接的话 Spring 会返回它自己那套 400 响应体（没有 code / msg 字段），
     * 而前端 request.ts 是判断 {@code body.code !== 0} 的 —— 拿到那种体只会提示一个
     * 取不到原因的"失败"。这里统一成和上面一样的 {@code code:10001 + errors{字段:消息}}。</p>
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public R handleMissingParameter(MissingServletRequestParameterException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        errors.put(ex.getParameterName(), "不能为空");
        return R.error(BaseCodeEnum.VALID_EXCEPTION.getCode(), BaseCodeEnum.VALID_EXCEPTION.getMsg()).put("errors", errors);
    }

    /**
     * 处理JSON解析异常
     */
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public R handleHttpMessageNotReadableException(org.springframework.http.converter.HttpMessageNotReadableException ex) {
        log.error("HttpMessageNotReadableException:", ex);
        return R.error(BaseCodeEnum.JSON_EXCEPTION.getCode(), BaseCodeEnum.JSON_EXCEPTION.getMsg()).put("errors", ex.getMessage());
    }

//    /**
//     * 处理所有其他异常
//     */
//    @ExceptionHandler(Exception.class)
//    public R handleException(Exception ex) {
//        log.error("Exception:", ex);
//        return R.error(BaseCodeEnum.UNKNOWN_EXCEPTION.getCode(), BaseCodeEnum.UNKNOWN_EXCEPTION.getMsg()).put("details", ex.getMessage());
//    }
}

package common.config;

import common.exception.BaseCodeEnum;
import common.exception.BaseException;
import common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
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

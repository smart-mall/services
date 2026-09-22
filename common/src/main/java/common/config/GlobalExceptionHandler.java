package common.config;

import common.exception.BaseCodeEnum;
import common.exception.BaseException;
import common.exception.ValidationException;
import common.utils.R;
import jakarta.servlet.http.HttpServletResponse;
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

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    /**
     * 业务异常。code 由抛出方指定，不再统一写死成 444 ——
     * 入参不合法抛出的是 {@code BaseCodeEnum.VALID_EXCEPTION}(10001)，
     * 其余兜底场景默认 {@code UNKNOWN_EXCEPTION}(10000)。
     */
    @ExceptionHandler(BaseException.class)
    public R handleException(BaseException e) {
        return R.error(e.getCode(), e.getMessage());
    }

    /**
     * 手写校验失败，产出的响应体和下面两条注解路径完全一样。
     *
     * <p>注解能表达的规则不该走这里（见 {@link ValidationException} 的说明），
     * 这里是跨字段、复合格式这类规则的统一出口 —— 目的就是让调用方只需要认一种形状。</p>
     */
    @ExceptionHandler(ValidationException.class)
    public R handleManualValidation(ValidationException e) {
        return R.error(e.getCode(), e.getMessage()).put("errors", e.getErrors());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R handleValidationException(MethodArgumentNotValidException ex) {
        BindingResult bindingResult = ex.getBindingResult();
        // LinkedHashMap 而不是 HashMap：errors 会被拼成一句话给用户看，顺序必须稳定
        Map<String, String> errors = new LinkedHashMap<>();

        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            errors.put(fieldError.getField(), describeFieldError(fieldError));
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
     * 处理 query 参数类型不对，例如 {@code pageNum=abc}、{@code brandId=xyz}。
     *
     * <p>绑定失败时 Spring 抛的是这个异常，不接的话返回它自己那套 400 响应体（同样没有
     * code / msg）。这一条覆盖了所有数字型查询参数，不用再逐个字段手写类型校验。</p>
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public R handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        errors.put(ex.getName(), typeMismatchMessage(ex.getValue()));
        return R.error(BaseCodeEnum.VALID_EXCEPTION.getCode(), BaseCodeEnum.VALID_EXCEPTION.getMsg()).put("errors", errors);
    }

    /**
     * 把"绑定失败"的字段错误换成面向用户的中文短句。
     *
     * <p>注解校验失败时 {@code getDefaultMessage()} 就是注解上写的中文，直接用；
     * 但类型转换失败没有走注解 —— 它是 {@code DataBinder} 抛的，Spring 给的默认消息
     * 是英文长句：</p>
     *
     * <pre>Failed to convert property value of type 'java.lang.String' to required type
     * 'java.lang.Integer' for property 'pageNum'; For input string: "abc"</pre>
     *
     * <p>同一个 {@code errors} 里一半中文一半英文、长度差好几倍，前端拼出来的提示
     * 没法看，所以统一换成短句，并带上被拒绝的原值 —— 这是用户唯一能对上号的线索。</p>
     */
    private static String describeFieldError(FieldError fieldError) {
        return fieldError.isBindingFailure()
                ? typeMismatchMessage(fieldError.getRejectedValue())
                : fieldError.getDefaultMessage();
    }

    /**
     * 类型转换失败的统一文案，形如 {@code 参数类型不正确：abc}。
     *
     * <p>原值截断到 {@value #MAX_REJECTED_VALUE_LENGTH} 个字符：这是直接把请求参数
     * 回显进响应体，不截断的话一个超长参数就能把提示撑成一屏。</p>
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
     * 请求体不是合法 JSON。
     *
     * <p>这里以前把 {@code ex.getMessage()} 直接塞进 {@code errors}，于是 {@code errors}
     * 是字符串而不是 Map —— 同一个字段在四条 handler 里有两种类型，调用方没法统一处理。
     * 现在统一成 Map；真实异常文本（含类名、字段路径）只进日志。</p>
     */
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public R handleHttpMessageNotReadableException(org.springframework.http.converter.HttpMessageNotReadableException ex) {
        log.error("HttpMessageNotReadableException:", ex);
        Map<String, String> errors = new LinkedHashMap<>();
        errors.put("body", "请求体不是合法的 JSON");
        return R.error(BaseCodeEnum.JSON_EXCEPTION.getCode(), BaseCodeEnum.JSON_EXCEPTION.getMsg()).put("errors", errors);
    }

    /**
     * 兜底。
     *
     * <p>以前这段是注释掉的，于是任何未预期异常（NPE、字段超长触发的
     * {@code DataIntegrityViolationException}…）都会走 Spring 的默认 500 响应体
     * {@code {timestamp,status,error,path}} —— 没有 code、没有 msg。而调用方是判断
     * {@code body.code} 的，结果就是"只显示失败、取不到原因"。renren-fast 有兜底、
     * 其余模块没有，两半边表现还不一样，所以必须补上。</p>
     *
     * <p><b>Spring MVC 自己的异常要放行状态码</b>：405 方法不支持、415 媒体类型不支持
     * 这类异常带着语义化的 HTTP 状态码，把它们吞成 200 + 10000 会让调用方分不清
     * "我调错了"和"服务挂了"。这里把状态码透传出去，响应体仍然补成统一的 {@code {code, msg}}。</p>
     *
     * <p>真实异常只进日志：响应体不回显 {@code ex.getMessage()}，那是内部实现细节
     * （表名、SQL、类名），既帮不到调用方也泄露实现。</p>
     */
    @ExceptionHandler(Exception.class)
    public R handleUnexpectedException(Exception ex, HttpServletResponse response) {
        if (ex instanceof ErrorResponse errorResponse) {
            int status = errorResponse.getStatusCode().value();
            log.warn("请求不被接受: status={} {}", status, ex.getMessage());
            response.setStatus(status);
            return R.error(status, "请求方式、路径或内容类型不被接受，请检查调用方式");
        }
        log.error("未预期异常:", ex);
        return R.error(BaseCodeEnum.UNKNOWN_EXCEPTION.getCode(), BaseCodeEnum.UNKNOWN_EXCEPTION.getMsg());
    }
}

package common.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 字段级校验失败：除 code/msg 外，data 里还带字段明细（字段名 → 中文消息）。
 *
 * <p>只用于注解表达不了的校验：跨字段、复合格式、依赖服务端状态这几类；
 * 必填、长度、格式、范围、单字段正则一律用 {@code jakarta.validation.constraints} 配合 {@code @Valid}。
 *
 * <p>与注解那两条路径最终产出的响应体一致：
 * {@code {code:10001, msg:"参数格式校验失败", data:{"字段名":"中文消息"}}}。
 */
public class ValidationException extends BaseException {

    /** 字段名 → 中文消息，不可修改 */
    private final Map<String, String> errors;

    /**
     * 用单个字段的校验失败构造。
     *
     * @param field   字段名
     * @param message 中文提示消息
     */
    public ValidationException(String field, String message) {
        this(Map.of(field, message));
    }

    /**
     * 用多个字段的校验失败构造。
     *
     * <p>复制进 LinkedHashMap，保证响应里字段顺序稳定。
     *
     * @param errors 字段名到中文消息的映射
     */
    public ValidationException(Map<String, String> errors) {
        super(BaseCodeEnum.VALID_EXCEPTION);
        this.errors = Collections.unmodifiableMap(new LinkedHashMap<>(errors));
    }

    /**
     * 返回字段级错误明细。
     *
     * @return 字段名到中文消息的不可修改映射
     */
    public Map<String, String> getErrors() {
        return errors;
    }
}

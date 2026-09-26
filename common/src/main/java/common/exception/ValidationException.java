package common.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 字段级校验失败：除了 code/msg，data 里还带字段明细（字段名 → 中文消息）。
 *
 * <p>和 {@link BaseException} 只差这一个字段，但值得单独一个类：{@code BaseException}
 * 是全项目的通用业务异常（11000 商品上架、15003 密码错、17000 订单不存在…），
 * 给它加一个"只有校验失败才该填"的字段，会让每个抛出点都得先想"我要不要填 errors"。</p>
 *
 * <p><b>什么时候用它：注解表达不了的校验。</b>注解能表达的（必填、长度、格式、范围、
 * 单字段正则）一律用 {@code jakarta.validation.constraints}，由 {@code @Valid} 触发；
 * 只有跨字段、复合格式、依赖服务端状态这几类才手写并抛这个异常。</p>
 *
 * <p>它和注解那两条路径（{@code MethodArgumentNotValidException}、
 * {@code ConstraintViolationException}）最终产出的响应体完全一致：</p>
 *
 * <pre>{@code {code:10001, msg:"参数格式校验失败", data:{"字段名":"中文消息"}}}</pre>
 *
 * <p>字段明细和其它接口一样放 {@code data}，调用方只认一种响应体形状。</p>
 */
public class ValidationException extends BaseException {

    private final Map<String, String> errors;

    /** 单字段 */
    public ValidationException(String field, String message) {
        this(Map.of(field, message));
    }

    /** 多字段。复制进 LinkedHashMap，保证响应里字段顺序稳定 */
    public ValidationException(Map<String, String> errors) {
        super(BaseCodeEnum.VALID_EXCEPTION);
        this.errors = Collections.unmodifiableMap(new LinkedHashMap<>(errors));
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}

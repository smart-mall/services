package common.utils;

import common.exception.BaseCodeEnum;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应结构 {@code {code, msg, data}}，所有接口都返回它。
 *
 * <p>data 的类型由类型参数确定。以前它是 {@code HashMap<String, Object>}：取数据靠
 * {@code getData("key", new TypeReference<>(){})} 在运行期反序列化，key 写错编译期不报错。</p>
 *
 * @param <T> 业务数据类型；无返回值时用 {@link Void}
 */
@Data
@NoArgsConstructor
public class R<T> {

    /** 成功码。业务错误码见 {@link BaseCodeEnum} */
    public static final int SUCCESS_CODE = 0;

    private static final String SUCCESS_MSG = "success";

    private int code;

    private String msg;

    private T data;

    private R(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    /** 成功，无返回数据 */
    public static R<Void> ok() {
        return new R<>(SUCCESS_CODE, SUCCESS_MSG, null);
    }

    /** 成功并携带数据 */
    public static <T> R<T> ok(T data) {
        return new R<>(SUCCESS_CODE, SUCCESS_MSG, data);
    }

    /** 失败，码和文案都取自枚举 */
    public static <T> R<T> error(BaseCodeEnum baseCodeEnum) {
        return new R<>(baseCodeEnum.getCode(), baseCodeEnum.getMsg(), null);
    }

    /** 失败，码取自枚举、data 带明细（字段级校验失败走这个） */
    public static <T> R<T> error(BaseCodeEnum baseCodeEnum, T data) {
        return new R<>(baseCodeEnum.getCode(), baseCodeEnum.getMsg(), data);
    }

    /** 失败，码和文案都由调用方给出，用于转发下游服务的错误 */
    public static <T> R<T> error(int code, String msg) {
        return new R<>(code, msg, null);
    }

    /** 失败并带数据，码和文案由调用方给出 */
    public static <T> R<T> error(int code, String msg, T data) {
        return new R<>(code, msg, data);
    }
}

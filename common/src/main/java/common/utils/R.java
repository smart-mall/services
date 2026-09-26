package common.utils;

import common.exception.BaseCodeEnum;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应结构 {@code {code, msg, data}}，所有接口都返回它。
 *
 * <p>{@code data} 的类型由类型参数确定，取数据不需要运行期反序列化，字段名写错编译期就会报错。
 *
 * @param <T> 业务数据类型；无返回值时用 {@link Void}
 */
@Data
@NoArgsConstructor
public class R<T> {

    /** 成功码，业务错误码见 {@link BaseCodeEnum}。 */
    public static final int SUCCESS_CODE = 0;

    private static final String SUCCESS_MSG = "success";

    /** 业务状态码，0 表示成功，其余见 {@link BaseCodeEnum}。 */
    private int code;

    /** 提示文案，成功时为 {@code "success"}。 */
    private String msg;

    /** 业务数据，类型由类型参数决定。 */
    private T data;

    private R(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    /**
     * 构造不带数据的成功响应。
     *
     * @return code 为 0、data 为 {@code null} 的响应
     */
    public static R<Void> ok() {
        return new R<>(SUCCESS_CODE, SUCCESS_MSG, null);
    }

    /**
     * 构造携带数据的成功响应。
     *
     * @param data 业务数据，允许为 {@code null}
     * @param <T>  业务数据类型
     * @return code 为 0 的响应
     */
    public static <T> R<T> ok(T data) {
        return new R<>(SUCCESS_CODE, SUCCESS_MSG, data);
    }

    /**
     * 构造失败响应，码与文案都取自枚举。
     *
     * @param baseCodeEnum 错误码枚举
     * @param <T>          业务数据类型
     * @return data 为 {@code null} 的失败响应
     */
    public static <T> R<T> error(BaseCodeEnum baseCodeEnum) {
        return new R<>(baseCodeEnum.getCode(), baseCodeEnum.getMsg(), null);
    }

    /**
     * 构造带明细的失败响应，字段级校验失败走这个。
     *
     * @param baseCodeEnum 错误码枚举
     * @param data         明细数据，例如字段名到中文消息的映射
     * @param <T>          明细数据类型
     * @return 携带明细的失败响应
     */
    public static <T> R<T> error(BaseCodeEnum baseCodeEnum, T data) {
        return new R<>(baseCodeEnum.getCode(), baseCodeEnum.getMsg(), data);
    }

    /**
     * 构造失败响应，码与文案由调用方给出，用于转发下游服务的错误。
     *
     * @param code 业务状态码
     * @param msg  提示文案
     * @param <T>  业务数据类型
     * @return data 为 {@code null} 的失败响应
     */
    public static <T> R<T> error(int code, String msg) {
        return new R<>(code, msg, null);
    }

    /**
     * 构造带明细的失败响应，码与文案由调用方给出。
     *
     * @param code 业务状态码
     * @param msg  提示文案
     * @param data 明细数据
     * @param <T>  明细数据类型
     * @return 携带明细的失败响应
     */
    public static <T> R<T> error(int code, String msg, T data) {
        return new R<>(code, msg, data);
    }
}

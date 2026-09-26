package common.exception;

/**
 * 业务异常，携带 {@link BaseCodeEnum} 中定义的错误码。
 *
 * <p>code 由抛出方指定，未指定时按 {@link BaseCodeEnum#UNKNOWN_EXCEPTION} 处理；
 * 由 {@code GlobalExceptionHandler} 统一转成 {@code R}，HTTP 状态码保持 200。
 */
public class BaseException extends RuntimeException {

    /** 业务状态码，取值见 {@link BaseCodeEnum}。 */
    private final int code;

    /**
     * 不指定 code，按"未知异常"处理，适用于"删除失败""远程服务调用失败"这类兜底场景。
     *
     * @param message 提示文案
     */
    public BaseException(String message) {
        this(BaseCodeEnum.UNKNOWN_EXCEPTION.getCode(), message);
    }

    /**
     * code 与 message 都取自枚举，适用于文案已经固定的错误。
     *
     * @param baseCodeEnum 错误码枚举
     */
    public BaseException(BaseCodeEnum baseCodeEnum) {
        this(baseCodeEnum.getCode(), baseCodeEnum.getMsg());
    }

    /**
     * code 取自枚举，message 由调用方补充上下文。
     *
     * @param baseCodeEnum 错误码枚举
     * @param message      提示文案
     */
    public BaseException(BaseCodeEnum baseCodeEnum, String message) {
        this(baseCodeEnum.getCode(), message);
    }

    /**
     * code 与 message 都由调用方给出，用于转发下游服务的错误码。
     *
     * @param code    业务状态码
     * @param message 提示文案
     */
    public BaseException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 返回业务状态码。
     *
     * @return 业务状态码
     */
    public int getCode() {
        return code;
    }
}

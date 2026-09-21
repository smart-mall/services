package common.exception;

/**
 * 业务异常。
 *
 * <p>带 code 的原因：以前所有 BaseException 都被 GlobalExceptionHandler 统一返回硬编码的
 * {@code 444} —— 那个数字既不在 BaseCodeEnum 里也没有任何语义，而同样是"入参不合法"，
 * 参数校验走的却是 {@code 10001}，前端得为同一类错误写两个分支。
 * 现在 code 由抛出方指定，不指定时用 {@link BaseCodeEnum#UNKNOWN_EXCEPTION}。</p>
 */
public class BaseException extends RuntimeException {

    private final int code;

    /**
     * 不指定 code，按"未知异常"处理。适用于"删除失败""远程服务调用失败"这类兜底场景。
     */
    public BaseException(String message) {
        this(BaseCodeEnum.UNKNOWN_EXCEPTION.getCode(), message);
    }

    /**
     * code 和 message 都取自枚举。文案已经固定的错误用这个，
     * 省得在每个调用点把枚举里的 message 再抄一遍 —— 抄一遍就有抄错的那天。
     */
    public BaseException(BaseCodeEnum baseCodeEnum) {
        this(baseCodeEnum.getCode(), baseCodeEnum.getMsg());
    }

    public BaseException(BaseCodeEnum baseCodeEnum, String message) {
        this(baseCodeEnum.getCode(), message);
    }

    public BaseException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}

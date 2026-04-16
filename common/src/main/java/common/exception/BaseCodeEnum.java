package common.exception;

import lombok.Getter;

@Getter
public enum BaseCodeEnum {
    UNKNOWN_EXCEPTION(100000, "未知异常"),
    VALID_EXCEPTION(100001, "参数格式校验失败"),
    JSON_EXCEPTION(100002, "JSON格式化异常"),
    PRODUCT_UP_EXCEPTION(110000, "商品上架异常");

    private final int code;
    private final String msg;
    BaseCodeEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

}

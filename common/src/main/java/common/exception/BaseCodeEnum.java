package common.exception;

import lombok.Getter;

@Getter
public enum BaseCodeEnum {
    UNKNOWN_EXCEPTION(10000, "未知异常"),
    VALID_EXCEPTION(10001, "参数格式校验失败"),
    JSON_EXCEPTION(10002, "JSON格式化异常"),
    PRODUCT_UP_EXCEPTION(11000, "商品上架异常"),
    TO_MANY_REQUEST(10003, "请求流量过大，请稍后再试"),

    SMS_CODE_EXCEPTION(10004,"验证码获取频率太高，请稍后再试"),
    USER_EXIST_EXCEPTION(15001,"存在相同的用户"),
    PHONE_EXIST_EXCEPTION(15002,"存在相同的手机号"),
    NO_STOCK_EXCEPTION(21000,"商品库存不足"),
    LOGINACCT_PASSWORD_EXCEPTION(15003,"账号或密码错误"),

    // 下面两个配合 HTTP 401 一起用。前端 request.ts 是看 HTTP 状态码 401 去清 token 的，
    // 不看 body 里的 code，所以状态码必须是真 401，body 里的 code 只是给人看日志用的。
    NOT_LOGIN_EXCEPTION(15004,"请先登录"),
    LOGIN_EXPIRED_EXCEPTION(15005,"登录已过期，请重新登录"),

    // 邮箱注册/登录用。15006 对应 member 的 EmailException（邮箱已被注册），
    // 15007 是「用邮箱+验证码登录」时该邮箱还没注册过。
    EMAIL_EXIST_EXCEPTION(15006,"存在相同的邮箱"),
    EMAIL_NOT_REGISTER_EXCEPTION(15007,"该邮箱尚未注册，请先注册"),
    ;


    private final int code;
    private final String msg;
    BaseCodeEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

}

package common.constant;


public class AuthServerConstant {

    public static final String SMS_CODE_CACHE_PREFIX = "sms:code:";

    /** 邮箱验证码的 key 前缀，用法同 {@link #SMS_CODE_CACHE_PREFIX} */
    public static final String EMAIL_CODE_CACHE_PREFIX = "email:code:";

    /** 当前登录会员。值是 Base64URL(UTF-8 JSON)，见 {@link common.utils.LoginUserUtils} */
    public static final String MEMBER_CLAIMS_HEADER = "X-Member-Claims";

    /** 当前登录的后台管理员，编码方式同 {@link #MEMBER_CLAIMS_HEADER} */
    public static final String ADMIN_HEADER = "X-Admin";

    /** 客户端 IP。经网关转发后下游看到的 remoteAddr 是网关自己 */
    public static final String CLIENT_IP_HEADER = "X-Client-IP";


}

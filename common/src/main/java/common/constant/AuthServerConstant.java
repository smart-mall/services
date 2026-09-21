package common.constant;


public class AuthServerConstant {

    public static final String SMS_CODE_CACHE_PREFIX = "sms:code:";

    /**
     * 邮箱验证码在 Redis 里的 key 前缀，用法和 {@link #SMS_CODE_CACHE_PREFIX} 完全一致：
     * value 是 {@code 验证码_写入时间戳}，防刷和一次性消费都靠它。
     */
    public static final String EMAIL_CODE_CACHE_PREFIX = "email:code:";

    /**
     * 网关验签通过之后注入的用户信息请求头。
     *
     * <p>登录态从 Session 换成 JWT 时把 {@code LOGIN_USER} 换成了这个头：
     * 之前的做法是 auth 往 HttpSession 写 {@code loginUser}，依赖 Spring Session 把
     * 会话共享到 Redis，所有服务再从 session 里读；现在头部请求由网关注入，业务服务只读头。</p>
     *
     * <p>值不是明文，是 Base64URL(UTF-8 JSON)，见 {@link common.utils.LoginUserUtils}。</p>
     */
    public static final String MEMBER_CLAIMS_HEADER = "X-Member-Claims";


}

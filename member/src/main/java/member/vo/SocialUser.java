package member.vo;

import lombok.Data;


/**
 * 微博 {@code oauth2/access_token} 接口的返回结果。
 *
 * <p>字段名与微博返回的 JSON 完全一致（含下划线），不能按 Java 命名习惯改写，否则反序列化取不到值。</p>
 */
@Data
public class SocialUser {

    /** 微博接口调用凭据，可直接冒用，禁止写进日志。 */
    private String access_token;

    /** access_token 的剩余有效期（秒）。 */
    private String remind_in;

    /** access_token 有效期，单位秒。 */
    private long expires_in;

    /** 微博用户 ID，member 服务按它识别社交账号。 */
    private String uid;

    /** 是否实名认证，取值为字符串形式的 true / false。 */
    private String isRealName;

}

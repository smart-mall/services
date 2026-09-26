package common.to;

import lombok.Data;

/**
 * 登录记录：auth 在登录成功后组装，经 Feign 交给 member 落库。
 */
@Data
public class LoginLogTo {

    /** Web 端登录。 */
    public static final Integer LOGIN_TYPE_WEB = 1;

    /** 登录会员 ID。 */
    private Long memberId;

    /** 客户端 IP，网关注入；取不到时为 {@code null}。 */
    private String ip;

    /** IP 归属城市；解析不出来时为 {@code null}，不影响这条记录落库。 */
    private String city;

    /** 登录类型，取值见 {@link #LOGIN_TYPE_WEB}。 */
    private Integer loginType;
}

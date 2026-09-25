package common.to;

import lombok.Data;

/**
 * 一条登录记录：auth 在登录成功之后组装，交给 member 落库。
 */
@Data
public class LoginLogTo {

    /** 登录类型：web */
    public static final Integer LOGIN_TYPE_WEB = 1;

    private Long memberId;

    /** 客户端 IP。网关注入，取不到时为 null */
    private String ip;

    /** IP 归属城市。解析不出来时为 null，不影响这条记录落库 */
    private String city;

    private Integer loginType;
}

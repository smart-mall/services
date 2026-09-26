package common.vo;

import lombok.Data;

/** 网关注入 {@code X-Admin} 时携带的后台管理员身份。 */
@Data
public class AdminResponseVo {

    /** 管理员 ID。 */
    private Long id;

    /** 管理员登录名。 */
    private String username;

}

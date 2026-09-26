package getway.vo;

import lombok.Data;

/**
 * 凭证校验返回的管理员身份，放在 {@code R.data} 里。
 */
@Data
public class AdminVerifyVo {

    /** 管理员 ID。 */
    private Long userId;

    /** 管理员登录名。 */
    private String username;
}

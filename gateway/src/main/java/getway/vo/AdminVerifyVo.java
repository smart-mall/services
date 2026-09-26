package getway.vo;

import lombok.Data;

/**
 * 凭证校验返回的管理员身份，放在 {@code R.data} 里。
 */
@Data
public class AdminVerifyVo {

    private Long userId;

    private String username;
}

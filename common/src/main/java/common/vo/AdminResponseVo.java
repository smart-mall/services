package common.vo;

import lombok.Data;

/** 网关注入 {@code X-Admin} 时携带的后台管理员身份。 */
@Data
public class AdminResponseVo {

    private Long id;

    private String username;

}

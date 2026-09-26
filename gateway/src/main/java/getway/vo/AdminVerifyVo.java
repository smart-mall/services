package getway.vo;

import lombok.Data;

/**
 * renren 凭证校验接口的响应。
 *
 * <p>形状是 {@code {code, msg, userId, username}} —— 身份平铺在顶层，不是我们的
 * {@code {code, msg, data}}。renren 属于第三方不能改，所以按它真实的形状建模，不套 {@code R}。</p>
 */
@Data
public class AdminVerifyVo {

    private int code;

    private String msg;

    private Long userId;

    private String username;
}

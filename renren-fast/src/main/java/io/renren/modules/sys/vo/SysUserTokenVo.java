package io.renren.modules.sys.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 给网关的凭证校验结果，放在 {@code data} 里。
 *
 * <p>原来是 {@code R.ok().put("userId", …).put("username", …)} 平铺在顶层，
 * 和全项目 {@code {code, msg, data}} 的形状不一致，网关那边只能用 Map 接。</p>
 */
@Data
@AllArgsConstructor
public class SysUserTokenVo {

    private Long userId;

    private String username;
}

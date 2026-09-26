/**
 * Copyright (c) 2016-2019 人人开源 All rights reserved.
 *
 * https://www.renren.io
 *
 * 版权所有，侵权必究！
 */

package io.renren.modules.sys.controller;

import io.renren.common.utils.R;
import io.renren.modules.sys.entity.SysUserEntity;
import io.renren.modules.sys.entity.SysUserTokenEntity;
import io.renren.modules.sys.service.ShiroService;
import io.renren.modules.sys.vo.SysUserTokenVo;
import org.apache.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 给网关用的接口：把 token 换成管理员身份。校验口径复用 {@link ShiroService}，和 Shiro 认登录一致。 */
@RestController
@RequestMapping("/sys/usertoken")
public class SysUserTokenController {

    private final ShiroService shiroService;

    public SysUserTokenController(ShiroService shiroService) {
        this.shiroService = shiroService;
    }

    /** 无效、过期、账号被锁定统一回 401，不区分原因 */
    @GetMapping("/verify")
    public R verify(@RequestHeader(value = "token", required = false) String token) {
        if (token == null || token.trim().isEmpty()) {
            return R.error(HttpStatus.SC_UNAUTHORIZED, "invalid token");
        }

        SysUserTokenEntity tokenEntity = shiroService.queryByToken(token);
        if (tokenEntity == null || tokenEntity.getExpireTime().getTime() < System.currentTimeMillis()) {
            return R.error(HttpStatus.SC_UNAUTHORIZED, "invalid token");
        }

        SysUserEntity user = shiroService.queryUser(tokenEntity.getUserId());
        if (user == null || user.getStatus() == 0) {
            return R.error(HttpStatus.SC_UNAUTHORIZED, "invalid token");
        }

        return R.ok().setData(new SysUserTokenVo(user.getUserId(), user.getUsername()));
    }

}

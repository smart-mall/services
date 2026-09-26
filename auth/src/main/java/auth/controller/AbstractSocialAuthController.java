package auth.controller;

import auth.feign.MemberFeignService;
import auth.service.LoginLogService;
import common.utils.ClientIpUtils;
import common.utils.JwtUtils;
import common.vo.MemberResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URI;

/**
 * 社交登录（微博 / QQ）两个 controller 的公共部分：用 code 换取用户信息、交 member 服务登录或自动注册、
 * 签发 JWT，最后 302 跳回前端。
 *
 * <p>这是唯一一类不能返回 JSON 的接口：请求由浏览器整页跳转发起，token 没法放进 Authorization 头，
 * 只能挂在 URL 上回传，因此会进浏览器历史、Referer 和服务端访问日志。
 */
@Slf4j
public abstract class AbstractSocialAuthController {

    protected final MemberFeignService memberFeignService;
    protected final JwtUtils jwtUtils;

    private final LoginLogService loginLogService;

    /** 前端地址。末尾斜杠在构造里已经去掉，免得拼出 //oauth/callback */
    protected final String frontUrl;

    /**
     * 构造器：前端地址末尾的斜杠在这里去掉，避免拼出 {@code //oauth/callback}。
     *
     * @param memberFeignService member 服务客户端
     * @param jwtUtils           JWT 签发工具
     * @param loginLogService    登录记录服务
     * @param frontUrl           前端地址，末尾带不带斜杠都可以
     */
    protected AbstractSocialAuthController(MemberFeignService memberFeignService,
                                           JwtUtils jwtUtils,
                                           LoginLogService loginLogService,
                                           String frontUrl) {
        this.memberFeignService = memberFeignService;
        this.jwtUtils = jwtUtils;
        this.loginLogService = loginLogService;
        this.frontUrl = frontUrl.endsWith("/") ? frontUrl.substring(0, frontUrl.length() - 1) : frontUrl;
    }

    /**
     * 登录成功：签发 JWT 并跳回前端的回调页，由前端把 token 存进 localStorage。
     *
     * @param user    member 服务返回的会员信息，{@code id} 为空时视为失败
     * @param channel 登录渠道标识，用于日志与失败原因，如 {@code weibo} / {@code qq}
     * @param request 当前请求，用于取客户端 IP
     * @return 302 跳转响应；用户信息不完整时跳回登录页
     */
    protected ResponseEntity<Void> toFrontWithToken(MemberResponseVo user, String channel,
                                                    HttpServletRequest request) {
        if (user == null || user.getId() == null) {
            log.error("{} 登录返回的用户信息不完整", channel);
            return toLoginPage(channel + "_user_missing");
        }

        // 密码哈希与微博令牌不能出这个类：MemberResponseVo 上的 @JsonIgnore 对 fastjson 不生效
        user.setPassword(null);
        user.setAccessToken(null);

        String token = jwtUtils.create(user);
        log.info("{} 登录成功: memberId={}", channel, user.getId());
        loginLogService.recordWebLogin(user.getId(), ClientIpUtils.currentIp(request));

        // JWT 是 Base64URL 字符集（字母数字 - _ 和点），本身就能直接放查询串，不用再编码
        // TODO: 接入一次性 code 换取 token（需前端配合 exchange 接口），避免 token 进 URL
        return redirect(frontUrl + "/oauth/callback?token=" + token);
    }

    /**
     * 跳回前端登录页并带上社交登录的失败原因。
     *
     * @param reason 失败原因标识，前端按它展示提示
     * @return 302 跳转响应
     */
    protected ResponseEntity<Void> toLoginPage(String reason) {
        return redirect(frontUrl + "/login?socialError=" + reason);
    }

    /**
     * 构造 302 跳转响应。
     *
     * @param url 目标地址
     * @return 302 跳转响应
     */
    protected ResponseEntity<Void> redirect(String url) {
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).build();
    }
}

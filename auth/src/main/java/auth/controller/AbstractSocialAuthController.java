package auth.controller;

import auth.feign.MemberFeignService;
import common.utils.JwtUtils;
import common.vo.MemberResponseVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URI;

/**
 * 社交登录（微博 / QQ）两个 controller 的公共部分。
 *
 * <p>两个渠道的流程一模一样：浏览器带着 code 跳到我们登记的 redirect_uri →
 * 用 code 换取用户信息 → 交给 member 服务按社交账号登录或自动注册 → 签发 JWT →
 * 302 跳回前端并把 token 挂在查询串上。只有"换取用户信息"那一段不同，
 * 所以提到基类共用，两个子类各自只留自己那段。</p>
 *
 * <p>这是唯一一类不能返回 JSON 的接口：请求是<b>浏览器整页跳转</b>过来的，
 * token 没法放在 Authorization 头里，只能挂在 URL 上回传给前端。</p>
 *
 * <p><b>已知取舍</b>：token 出现在 URL 查询串里，会进浏览器历史、Referer 和服务端访问日志。
 * 想做干净的话，正确做法是先往 Redis 写一个一次性的 code，跳回前端时只带 code，
 * 前端再 POST 换 token —— 需要前端配合加一个 exchange 调用，本轮先不做。</p>
 */
@Slf4j
public abstract class AbstractSocialAuthController {

    protected final MemberFeignService memberFeignService;
    protected final JwtUtils jwtUtils;

    /** 前端地址。末尾斜杠在构造里已经去掉，免得拼出 //oauth/callback */
    protected final String frontUrl;

    protected AbstractSocialAuthController(MemberFeignService memberFeignService,
                                           JwtUtils jwtUtils,
                                           String frontUrl) {
        this.memberFeignService = memberFeignService;
        this.jwtUtils = jwtUtils;
        this.frontUrl = frontUrl.endsWith("/") ? frontUrl.substring(0, frontUrl.length() - 1) : frontUrl;
    }

    /** 登录成功：签发 JWT 并跳回前端的回调页，由前端把 token 存进 localStorage */
    protected ResponseEntity<Void> toFrontWithToken(MemberResponseVo user, String channel) {
        if (user == null || user.getId() == null) {
            log.error("{} 登录返回的用户信息不完整", channel);
            return toLoginPage(channel + "_user_missing");
        }

        // 和 AccountAuthController#login 同理：这两个字段不能出这个类
        user.setPassword(null);
        user.setAccessToken(null);

        String token = jwtUtils.create(user);
        log.info("{} 登录成功: memberId={}", channel, user.getId());

        // JWT 是 Base64URL 字符集（字母数字 - _ 和点），本身就能直接放查询串，不用再编码
        return redirect(frontUrl + "/oauth/callback?token=" + token);
    }

    protected ResponseEntity<Void> toLoginPage(String reason) {
        return redirect(frontUrl + "/login?socialError=" + reason);
    }

    protected ResponseEntity<Void> redirect(String url) {
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).build();
    }
}

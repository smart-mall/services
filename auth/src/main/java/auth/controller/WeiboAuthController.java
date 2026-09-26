package auth.controller;

import auth.feign.MemberFeignService;
import auth.service.LoginLogService;
import auth.vo.SocialUser;
import com.alibaba.fastjson.JSON;
import common.utils.HttpUtils;
import common.utils.JwtUtils;
import common.utils.R;
import common.vo.MemberResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 微博登录：用授权 code 换取 access_token 与用户信息，再交 member 服务登录或自动注册。
 *
 * <p>{@link #WEIBO_REDIRECT_URI} 必须与微博开放平台后台登记的回调地址完全一致，改这里就要同步改后台；
 * 该路径不带 auth 前缀，只能靠网关的 {@code Host=auth.gulimall.com} 路由到达，不要动。
 */
@Slf4j
@RestController
public class WeiboAuthController extends AbstractSocialAuthController {

    /** 微博开放平台登记的回调地址，与后台登记不一致时换不到 access_token。 */
    private static final String WEIBO_REDIRECT_URI = "http://auth.gulimall.com/oauth2/weibo/success";

    public WeiboAuthController(MemberFeignService memberFeignService,
                               JwtUtils jwtUtils,
                               LoginLogService loginLogService,
                               @Value("${auth.front-url:http://localhost:5173}") String frontUrl) {
        super(memberFeignService, jwtUtils, loginLogService, frontUrl);
    }

    /**
     * 接收微博回调，完成换取用户信息、登录、签发 token 与跳回前端。
     *
     * <p>任何一步失败都跳回前端登录页并在 {@code socialError} 上带原因，不返回 JSON。</p>
     *
     * @param code    微博回调带回的授权码
     * @param request 当前请求，用于取客户端 IP
     * @return 302 跳转响应
     * @throws Exception 调用微博接口或读取响应体失败时抛出
     */
    @GetMapping(value = "/oauth2/weibo/success")
    public ResponseEntity<Void> weibo(@RequestParam("code") String code,
                                      HttpServletRequest request) throws Exception {

        Map<String, String> map = new HashMap<>();
        map.put("client_id", "1398918556");
        map.put("client_secret", "257b44041522cbad99701351865cde44");
        map.put("grant_type", "authorization_code");
        map.put("redirect_uri", WEIBO_REDIRECT_URI);
        map.put("code", code);

        // 1. 用授权 code 换取 access_token
        HttpResponse response = HttpUtils.doPost("https://api.weibo.com", "/oauth2/access_token",
                "post", new HashMap<>(), map, new HashMap<>());

        // 2. 非 200 说明 code 已失效或参数不对，换不到令牌
        if (response.getStatusLine().getStatusCode() != 200) {
            log.warn("微博换取 access_token 失败, status={}", response.getStatusLine().getStatusCode());
            return toLoginPage("weibo_token_failed");
        }

        String json = EntityUtils.toString(response.getEntity());
        SocialUser socialUser = JSON.parseObject(json, SocialUser.class);

        // 日志只记 uid：access_token 可直接冒用，不能落日志
        log.info("微博授权成功, uid={}", socialUser.getUid());

        // 3. 交给 member 服务按社交账号登录或自动注册
        R<MemberResponseVo> oauthLogin = memberFeignService.oauthLogin(socialUser);
        if (oauthLogin.getCode() != 0) {
            log.warn("微博登录失败: {}", oauthLogin.getMsg());
            return toLoginPage("weibo_login_failed");
        }

        MemberResponseVo user = oauthLogin.getData();
        return toFrontWithToken(user, "weibo", request);
    }
}

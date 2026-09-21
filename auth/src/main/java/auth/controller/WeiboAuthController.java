package auth.controller;

import auth.feign.MemberFeignService;
import auth.vo.SocialUser;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import common.utils.HttpUtils;
import common.utils.JwtUtils;
import common.utils.R;
import common.vo.MemberResponseVo;
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
 * 微博登录。
 *
 * <p>{@link #WEIBO_REDIRECT_URI} 必须和微博开放平台后台登记的回调地址完全一致，
 * 改这里就要同步去后台改，否则换不到 access_token。也正因为如此，
 * 这个回调地址<b>不带 auth 前缀</b>（走不了网关的 {@code /api/auth/**} 路由，
 * 只能靠 {@code Host=auth.gulimall.com} 那条 Host 路由到达），这个路径不要动。</p>
 */
@Slf4j
@RestController
public class WeiboAuthController extends AbstractSocialAuthController {

    /** 微博开放平台登记的回调地址，改了要同步去微博后台改，否则换不到 access_token */
    private static final String WEIBO_REDIRECT_URI = "http://auth.gulimall.com/oauth2/weibo/success";

    public WeiboAuthController(MemberFeignService memberFeignService,
                               JwtUtils jwtUtils,
                               @Value("${auth.front-url:http://localhost:5173}") String frontUrl) {
        super(memberFeignService, jwtUtils, frontUrl);
    }

    @GetMapping(value = "/oauth2/weibo/success")
    public ResponseEntity<Void> weibo(@RequestParam("code") String code) throws Exception {

        Map<String, String> map = new HashMap<>();
        map.put("client_id", "1398918556");
        map.put("client_secret", "257b44041522cbad99701351865cde44");
        map.put("grant_type", "authorization_code");
        map.put("redirect_uri", WEIBO_REDIRECT_URI);
        map.put("code", code);

        //1、根据用户授权返回的code换取access_token
        HttpResponse response = HttpUtils.doPost("https://api.weibo.com", "/oauth2/access_token",
                "post", new HashMap<>(), map, new HashMap<>());

        //2、处理
        if (response.getStatusLine().getStatusCode() != 200) {
            log.warn("微博换取 access_token 失败, status={}", response.getStatusLine().getStatusCode());
            return toLoginPage("weibo_token_failed");
        }

        String json = EntityUtils.toString(response.getEntity());
        SocialUser socialUser = JSON.parseObject(json, SocialUser.class);

        // 别把 access_token 打进日志（原来这里是 System.out.println）
        log.info("微博授权成功, uid={}", socialUser.getUid());

        //3、让 member 服务按社交账号登录或自动注册
        R oauthLogin = memberFeignService.oauthLogin(socialUser);
        if (oauthLogin.getCode() != 0) {
            log.warn("微博登录失败: {}", oauthLogin.getMsg());
            return toLoginPage("weibo_login_failed");
        }

        MemberResponseVo user = oauthLogin.getData("data", new TypeReference<MemberResponseVo>() {});
        return toFrontWithToken(user, "weibo");
    }
}

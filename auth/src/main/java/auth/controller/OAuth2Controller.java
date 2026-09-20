package auth.controller;

import auth.feign.MemberFeignService;
import auth.vo.QQUserInfo;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * 社交登录（微博 / QQ）。
 *
 * <p>这是唯一一条不能返回 JSON 的链路：它是<b>浏览器整页跳转</b>过来的
 * （微博带着 code 跳到我们登记的 redirect_uri），所以只能 302 回去，
 * token 没法放在 Authorization 头里，只能挂在 URL 上回传给前端。</p>
 *
 * <p>和改造前比，变化有两点：</p>
 * <ol>
 *   <li>不再往 HttpSession 里写 loginUser（登录态已经是 JWT 了），改成签发 token 后
 *       带在 URL 上跳回前端，由前端调 setToken() 存进 localStorage。</li>
 *   <li>回跳目标从写死的 {@code http://gulimall.com} 改成可配置的 {@code auth.front-url}。</li>
 * </ol>
 *
 * <p><b>已知取舍</b>：token 出现在 URL 查询串里，会进浏览器历史、Referer 和服务端访问日志。
 * 想做干净的话，正确做法是先往 Redis 写一个一次性的 code，跳回前端时只带 code，
 * 前端再 POST 换 token —— 需要前端配合加一个 exchange 调用，本轮先不做。</p>
 */
@Slf4j
@RestController
public class OAuth2Controller {

    /** 微博开放平台登记的回调地址，改了要同步去微博后台改，否则换不到 access_token */
    private static final String WEIBO_REDIRECT_URI = "http://auth.gulimall.com/oauth2/weibo/success";

    private final MemberFeignService memberFeignService;
    private final JwtUtils jwtUtils;
    private final String frontUrl;

    public OAuth2Controller(MemberFeignService memberFeignService,
                            JwtUtils jwtUtils,
                            @Value("${auth.front-url:http://localhost:5173}") String frontUrl) {
        this.memberFeignService = memberFeignService;
        this.jwtUtils = jwtUtils;
        // 去掉末尾斜杠，免得拼出 //oauth/callback
        this.frontUrl = frontUrl.endsWith("/") ? frontUrl.substring(0, frontUrl.length() - 1) : frontUrl;
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

    @GetMapping(value = "/oauth2/qq/success")
    public ResponseEntity<Void> qq(@RequestParam("code") String code) throws Exception {
        log.info("进入qq登录: {}", code);

        Map<String, String> map = new HashMap<>();
        map.put("code", code);

        HttpResponse response = HttpUtils.doGet("https://qq.wch666.com", "/api/get_user_info.php",
                "get", new HashMap<>(), map);

        if (response.getStatusLine().getStatusCode() != 200) {
            log.warn("QQ 换取用户信息失败, status={}", response.getStatusLine().getStatusCode());
            return toLoginPage("qq_token_failed");
        }

        String json = EntityUtils.toString(response.getEntity(), "UTF-8");
        QQUserInfo qqUserInfo = JSON.parseObject(json, QQUserInfo.class);
        qqUserInfo.setToken(code);

        //让 member 服务按社交账号登录或自动注册
        R oauthLogin = memberFeignService.qqLogin(qqUserInfo);
        if (oauthLogin.getCode() != 0) {
            log.warn("QQ 登录失败: {}", oauthLogin.getMsg());
            return toLoginPage("qq_login_failed");
        }

        // 原来这里写的是 getData(new TypeReference<>() {})，靠赋值目标反推类型，
        // 显式写出来更清楚
        MemberResponseVo user = oauthLogin.getData("data", new TypeReference<MemberResponseVo>() {});
        return toFrontWithToken(user, "qq");
    }

    /** 登录成功：签发 JWT 并跳回前端的回调页，由前端把 token 存进 localStorage */
    private ResponseEntity<Void> toFrontWithToken(MemberResponseVo user, String channel) {
        if (user == null || user.getId() == null) {
            log.error("{} 登录返回的用户信息不完整", channel);
            return toLoginPage(channel + "_user_missing");
        }

        // 和 AuthController#login 同理：这两个字段不能出这个类
        user.setPassword(null);
        user.setAccessToken(null);

        String token = jwtUtils.create(user);
        log.info("{} 登录成功: memberId={}", channel, user.getId());

        // JWT 是 Base64URL 字符集（字母数字 - _ 和点），本身就能直接放查询串，不用再编码
        return redirect(frontUrl + "/oauth/callback?token=" + token);
    }

    private ResponseEntity<Void> toLoginPage(String reason) {
        return redirect(frontUrl + "/login?socialError=" + reason);
    }

    private ResponseEntity<Void> redirect(String url) {
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).build();
    }
}

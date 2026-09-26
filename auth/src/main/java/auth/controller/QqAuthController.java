package auth.controller;

import auth.feign.MemberFeignService;
import auth.service.LoginLogService;
import auth.vo.QQUserInfo;
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
 * QQ 登录。
 *
 * <p>和微博一样是浏览器整页跳转的回调，路径同样不带 auth 前缀，靠网关的
 * {@code Host=auth.gulimall.com} 那条 Host 路由到达。</p>
 *
 * <p>注意这里取用户信息走的是第三方中转域名 {@code qq.wch666.com}（不是腾讯官方接口），
 * 它一旦不可用这个登录方式就整体失效 —— 这是改造前就有的依赖，没有动。</p>
 */
@Slf4j
@RestController
public class QqAuthController extends AbstractSocialAuthController {

    public QqAuthController(MemberFeignService memberFeignService,
                            JwtUtils jwtUtils,
                            LoginLogService loginLogService,
                            @Value("${auth.front-url:http://localhost:5173}") String frontUrl) {
        super(memberFeignService, jwtUtils, loginLogService, frontUrl);
    }

    @GetMapping(value = "/oauth2/qq/success")
    public ResponseEntity<Void> qq(@RequestParam("code") String code,
                                   HttpServletRequest request) throws Exception {
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
        R<MemberResponseVo> oauthLogin = memberFeignService.qqLogin(qqUserInfo);
        if (oauthLogin.getCode() != 0) {
            log.warn("QQ 登录失败: {}", oauthLogin.getMsg());
            return toLoginPage("qq_login_failed");
        }

        // 原来这里写的是 getData(new TypeReference<>() {})，靠赋值目标反推类型，
        // 显式写出来更清楚
        MemberResponseVo user = oauthLogin.getData();
        return toFrontWithToken(user, "qq", request);
    }
}

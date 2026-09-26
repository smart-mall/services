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
 * QQ 登录：用授权 code 从第三方中转域名换取用户信息，再交 member 服务登录或自动注册。
 *
 * <p>取用户信息走的是第三方中转域名 {@code qq.wch666.com}（不是腾讯官方接口），它一旦不可用
 * 这个登录方式就整体失效；回调路径同样不带 auth 前缀，靠网关的 {@code Host=auth.gulimall.com} 路由到达。
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

    /**
     * 接收 QQ 回调，完成换取用户信息、登录、签发 token 与跳回前端。
     *
     * @param code    QQ 回调带回的授权码，同时作为用户信息接口的入参
     * @param request 当前请求，用于取客户端 IP
     * @return 302 跳转响应
     * @throws Exception 调用第三方接口或读取响应体失败时抛出
     */
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

        // 必须显式指定 UTF-8：昵称等字段是中文，按默认编码解析会乱码
        String json = EntityUtils.toString(response.getEntity(), "UTF-8");
        QQUserInfo qqUserInfo = JSON.parseObject(json, QQUserInfo.class);
        qqUserInfo.setToken(code);

        // 交给 member 服务按社交账号登录或自动注册
        R<MemberResponseVo> oauthLogin = memberFeignService.qqLogin(qqUserInfo);
        if (oauthLogin.getCode() != 0) {
            log.warn("QQ 登录失败: {}", oauthLogin.getMsg());
            return toLoginPage("qq_login_failed");
        }

        MemberResponseVo user = oauthLogin.getData();
        return toFrontWithToken(user, "qq", request);
    }
}

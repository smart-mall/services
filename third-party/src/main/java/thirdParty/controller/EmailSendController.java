package thirdParty.controller;

import com.alibaba.fastjson.JSON;
import common.utils.HttpUtils;
import common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 邮箱验证码的发送，走 Resend 的 HTTP API。
 *
 * <p>和 {@code SmsSendController} 是同一个定位：auth 负责生成/存取/校验验证码，
 * 这里只负责"把验证码真正发出去"。</p>
 *
 * <p>调用方式是 {@code POST https://api.resend.com/emails}，body 是 JSON。
 * 这里用的是 {@link HttpUtils#doPost(String, String, String, Map, Map, byte[])} 这个 byte[] 重载，
 * 而不是 String 重载 —— 后者内部是 {@code new StringEntity(body, "utf-8")}，
 * 会把 Content-Type 强制设成 {@code text/plain; charset=UTF-8}，和我们要的 application/json 打架；
 * ByteArrayEntity 不设置 contentType，所以下面 header 里那个 application/json 才是唯一的。</p>
 *
 * <p><b>发件人</b>：配置项 {@code resend.from} 用的是自有域名下的 {@code notify@lxpavilion.top}，
 * 前提是该域名已在 Resend 后台验证通过（SPF/DKIM 那几条 DNS 记录配好），没验证过会返回 403。
 * 验证过的域名可以发给任意收件人；如果哪天换回 Resend 自带的 {@code onboarding@resend.dev}，
 * 则会被限制成只能发给你注册 Resend 时用的那个邮箱。</p>
 */
@Slf4j
@RestController
@RequestMapping(value = "/thirdParty/email")
public class EmailSendController {

    private static final String RESEND_HOST = "https://api.resend.com";
    private static final String RESEND_PATH = "/emails";

    private final String apiKey;
    private final String from;

    public EmailSendController(@Value("${resend.api-key}") String apiKey,
                               @Value("${resend.from:onboarding@resend.dev}") String from) {
        this.apiKey = apiKey;
        this.from = from;
    }

    /**
     * 发送邮箱验证码。
     *
     * <p>返回 {@code R.error} 而不是像短信那样一律 {@code R.ok()}：auth 那边是靠
     * {@code r.getCode() != 0} 判断远程调用是否成功的，短信那个实现把异常吞掉、
     * 永远返回成功，导致发信失败时前端还以为验证码已经发出去了。</p>
     */
    @GetMapping(value = "/sendCode")
    public R sendCode(@RequestParam("email") String email, @RequestParam("code") String code) {
        log.info("发送邮箱验证码: {}--{}", email, code);

        Map<String, Object> payload = new HashMap<>();
        payload.put("from", from);
        payload.put("to", List.of(email));
        payload.put("subject", "【谷粒商城】登录验证码");
        payload.put("html", buildHtml(code));

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + apiKey);
        headers.put("Content-Type", "application/json");

        try {
            // byte[] 重载：ByteArrayEntity 不设置 contentType，Content-Type 完全由上面的 header 决定
            HttpResponse response = HttpUtils.doPost(RESEND_HOST, RESEND_PATH, "POST",
                    headers, new HashMap<>(), JSON.toJSONBytes(payload));
            String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
            int status = response.getStatusLine().getStatusCode();
            log.info("Resend 响应: status={}, body={}", status, responseBody);

            if (status != 200) {
                // 401 一般是 key 不对；403 通常是上面那段测试限制；422 是参数不合法；429 是限流
                return R.error("邮件发送失败(" + status + "): " + responseBody);
            }
        } catch (Exception e) {
            log.error("发送邮箱验证码异常", e);
            return R.error("邮件发送异常: " + e.getMessage());
        }

        return R.ok();
    }

    private String buildHtml(String code) {
        return "<p>您的验证码是：<b style=\"font-size:18px\">" + code + "</b></p>"
                + "<p>5 分钟内有效，请勿泄露给他人。</p>";
    }
}

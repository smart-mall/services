package thirdParty.controller;

import com.alibaba.fastjson.JSON;
import common.utils.HttpUtils;
import common.exception.BaseCodeEnum;
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
 * 邮箱验证码发送接口，通过 Resend 的 HTTP API 投递；验证码本身的生成与校验在 auth 服务。
 *
 * <p>必须用 {@link HttpUtils#doPost(String, String, String, Map, Map, byte[])} 的 byte[] 重载：
 * String 重载内部是 {@code new StringEntity(body, "utf-8")}，会把 Content-Type 强制设成
 * {@code text/plain; charset=UTF-8}；ByteArrayEntity 不设置 contentType，header 里的
 * {@code application/json} 才不会被覆盖。</p>
 *
 * <p>发件人取自 {@code resend.from}，需为已在 Resend 后台完成 SPF/DKIM 验证的域名邮箱，否则返回 403。</p>
 */
@Slf4j
@RestController
@RequestMapping(value = "/thirdParty/email")
public class EmailSendController {

    private static final String RESEND_HOST = "https://api.resend.com";
    private static final String RESEND_PATH = "/emails";

    private final String apiKey;
    private final String from;

    /**
     * 注入 Resend 凭据与发件人地址。
     *
     * @param apiKey Resend API Key，作为 {@code Authorization: Bearer} 的值
     * @param from   发件人地址；未配置 {@code resend.from} 时默认 {@code onboarding@resend.dev}，
     *               该地址只能发往注册 Resend 时使用的邮箱
     */
    public EmailSendController(@Value("${resend.api-key}") String apiKey,
                               @Value("${resend.from:onboarding@resend.dev}") String from) {
        this.apiKey = apiKey;
        this.from = from;
    }

    /**
     * 发送邮箱验证码。
     *
     * <p>失败时返回 {@code R.error}：auth 靠 {@code r.getCode() != 0} 判断远程调用是否成功，
     * 一律返回成功会让发信失败时前端误以为验证码已发出。
     *
     * @param email 收件人邮箱，直接作为 Resend 请求体的 {@code to} 字段
     * @param code  验证码明文，会被拼进邮件 HTML 正文
     * @return 发送成功返回 {@code R.ok()}；HTTP 状态非 200 或调用异常时返回
     *         {@link BaseCodeEnum#EMAIL_SEND_EXCEPTION}
     */
    @GetMapping(value = "/sendCode")
    public R<Void> sendCode(@RequestParam("email") String email, @RequestParam("code") String code) {
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
            // 用 byte[] 重载：Content-Type 完全由上面的 header 决定，不会被请求实体覆盖
            HttpResponse response = HttpUtils.doPost(RESEND_HOST, RESEND_PATH, "POST",
                    headers, new HashMap<>(), JSON.toJSONBytes(payload));
            String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
            int status = response.getStatusLine().getStatusCode();
            log.info("Resend 响应: status={}, body={}", status, responseBody);

            if (status != 200) {
                // 401 多为 key 无效；403 多为发件域名未验证；422 为参数不合法；429 为触发限流
                log.error("邮件发送失败: status={}, body={}", status, responseBody);
                return R.error(BaseCodeEnum.EMAIL_SEND_EXCEPTION);
            }
        } catch (Exception e) {
            log.error("发送邮箱验证码异常", e);
            return R.error(BaseCodeEnum.EMAIL_SEND_EXCEPTION);
        }

        return R.ok();
    }

    private String buildHtml(String code) {
        return "<p>您的验证码是：<b style=\"font-size:18px\">" + code + "</b></p>"
                + "<p>5 分钟内有效，请勿泄露给他人。</p>";
    }
}

package thirdParty.controller;

import common.utils.HttpUtils;
import common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;


/**
 * 短信验证码发送接口，通过国阳云短信 API 投递；验证码本身的生成与校验在 auth 服务。
 *
 * <p>发送结果不影响响应：异常只记日志，一律返回 {@code R.ok()}，调用方无法从返回值判断短信是否送达。</p>
 */
@RestController
@RequestMapping(value = "/thirdParty/sms")
@Slf4j
public class SmsSendController {

    /**
     * 发送短信验证码。
     *
     * <p>捕获异常后只记日志并返回 {@code R.ok()}，因此 {@code r.getCode()} 为 0 只代表接口被调用过。
     *
     * @param mobile 接收短信的手机号
     * @param code   验证码明文，作为模板参数 {@code **code**} 传入
     * @param time   验证码有效期，单位分钟，作为模板参数 {@code **minute**} 传入
     * @return 恒返回 {@code R.ok()}，不代表短信已实际送达
     */
    @GetMapping(value = "/sendCode")
    public R<Void> sendCode(@RequestParam("mobile") String mobile, @RequestParam("code") String code, @RequestParam("time") Integer time) {
        log.info("发送验证码: {}--{}--{}",  mobile, code, time);

        String host = "https://gyytz.market.alicloudapi.com";
        String path = "/sms/smsSend";
        String method = "POST";
        String appcode = "9450540793294f108804896a6d4e2e5b";
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "APPCODE " + appcode);
        Map<String, String> querys = new HashMap<>();
        querys.put("mobile", mobile);
        querys.put("param", "**code**:" + code + ",**minute**:" + time);

        // smsSignId 是短信签名 ID，templateId 是短信模板 ID，均需在国阳云控制台申请
        querys.put("smsSignId", "2e65b1bb3d054466b82f0c9d125465e2");
        querys.put("templateId", "908e94ccf08b4476ba6c876d13f084ad");
        Map<String, String> bodys = new HashMap<>();


        try {
            HttpResponse response = HttpUtils.doPost(host, path, method, headers, querys, bodys);
            String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
            log.info("响应体内容: {}", responseBody);
        } catch (Exception e) {
            log.error("发送验证码异常", e);
        }

        return R.ok();
    }


}

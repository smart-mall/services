package auth.feign;

import common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


/**
 * third-party 服务的 Feign 客户端：真正把短信和邮件发出去，auth 只负责生成验证码与防刷。
 */
@FeignClient("third-party")
public interface ThirdPartFeignService {

    /**
     * 发送短信验证码。
     *
     * <p>短信通道无论成功失败都返回 {@code R.ok()}，所以 {@code code} 为 0 只代表接口被调用过，
     * 不代表短信已送达。</p>
     *
     * @param mobile 接收手机号
     * @param code   验证码内容
     * @param time   验证码有效期，单位分钟
     * @return 恒返回 {@code code:0}
     */
    @GetMapping(value = "/thirdParty/sms/sendCode")
    R<Void> sendCode(@RequestParam("mobile") String mobile, @RequestParam("code") String code, @RequestParam("time") int time);

    /**
     * 发送邮箱验证码，third-party 那边用 Resend 发信。
     *
     * <p>与短信接口不同：发信失败时返回 {@code R.error} 而不是一律 {@code R.ok()}，调用方能拿到真实
     * 失败原因（key 不对 401 / 域名没验证 403 / 参数不合法 422 / 限流 429）。</p>
     *
     * @param email 接收邮箱
     * @param code  验证码内容
     * @return 成功返回 {@code code:0}；失败时为对应的错误码与原因
     */
    @GetMapping(value = "/thirdParty/email/sendCode")
    R<Void> emailSendCode(@RequestParam("email") String email, @RequestParam("code") String code);

}

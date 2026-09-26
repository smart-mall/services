package auth.feign;

import common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @Description:
 * @Created: with IntelliJ IDEA.
 * @author: 夏沫止水
 * @createTime: 2020-06-27 10:10
 **/

@FeignClient("third-party")
public interface ThirdPartFeignService {

    @GetMapping(value = "/thirdParty/sms/sendCode")
    R<Void> sendCode(@RequestParam("mobile") String mobile, @RequestParam("code") String code, @RequestParam("time") int time);

    /**
     * 发邮箱验证码，third-party 那边用 Resend 发信。
     *
     * <p>和上面的短信接口有一点不同：这个接口发信失败时返回的是 {@code R.error}，
     * 而不是像短信那样一律 {@code R.ok()}，所以调用方能拿到真实的失败原因
     * （key 不对 401 / 域名没验证 403 / 参数不合法 422 / 限流 429）。</p>
     */
    @GetMapping(value = "/thirdParty/email/sendCode")
    R<Void> emailSendCode(@RequestParam("email") String email, @RequestParam("code") String code);

}

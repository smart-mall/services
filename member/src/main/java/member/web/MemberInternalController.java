package member.web;

import common.utils.LoginUserUtils;
import common.utils.R;
import common.vo.MemberResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import member.service.MemberService;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * 会员的服务间接口。
 *
 * <p><b>路径刻意不带 member 前缀</b>：网关的 member-route 是 {@code Path=/api/member/**}，
 * 所以 {@code /internal/member/**} 经网关打不到本服务（会落到 admin-route 变成 404），
 * 浏览器就够不着。调用方 auth 走 Feign 直连，不受网关路由限制。
 * 同一个做法见 seckill 的 {@code SeckillInternalController}（挂在 {@code /sku/seckill/**}）。</p>
 *
 * <p><b>身份仍从 {@code X-Member-Claims} 取</b>（网关注入，FeignConfig 会自动转发到下游）：
 * gl-com.yml 把服务端口映射到了宿主机，绕过网关直连仍能伪造这个头 —— 那是项目已知的缺口。
 * 但即便直连，也只能改到自己的号，改不了别人的。</p>
 */
@RestController
@RequestMapping("/internal/member")
public class MemberInternalController {

    private final MemberService memberService;

    public MemberInternalController(MemberService memberService) {
        this.memberService = memberService;
    }

    /**
     * 换绑手机号，由 auth 在验证码校验通过之后调用。
     *
     * <p>验证码不在这里验 —— 它的存取、防刷和一次性消费都收在 auth 的 VerifyCodeUtils 里，
     * 复制一套 key 格式过来只会让两边慢慢漂开。手机号已被别人绑定时返回
     * {@code MOBILE_IN_USE(15006)}。</p>
     */
    @PutMapping("/mobile")
    public R changeMobile(@RequestParam("mobile") String mobile,
                          HttpServletRequest request,
                          HttpServletResponse response) throws IOException {
        Long memberId = currentMemberId(request, response);
        if (memberId == null) {
            return null;
        }

        memberService.changeMobile(memberId, mobile);
        return R.ok();
    }

    /** 换绑邮箱，语义同 {@link #changeMobile}，占用时返回 {@code EMAIL_IN_USE(15007)} */
    @PutMapping("/email")
    public R changeEmail(@RequestParam("email") String email,
                         HttpServletRequest request,
                         HttpServletResponse response) throws IOException {
        Long memberId = currentMemberId(request, response);
        if (memberId == null) {
            return null;
        }

        memberService.changeEmail(memberId, email);
        return R.ok();
    }

    /** 当前登录会员 id；未登录时写好 401 + JSON 并返回 null */
    private Long currentMemberId(HttpServletRequest request, HttpServletResponse response) throws IOException {
        MemberResponseVo user = LoginUserUtils.currentUser(request);
        if (user == null || user.getId() == null) {
            LoginUserUtils.writeUnauthorized(response);
            return null;
        }
        return user.getId();
    }
}

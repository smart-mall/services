package auth.feign;


import auth.vo.QQUserInfo;
import auth.vo.SocialUser;
import auth.vo.UserEmailRegisterVo;
import auth.vo.UserLoginVo;
import auth.vo.UserRegisterVo;
import common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @Description:
 * @Created: with IntelliJ IDEA.
 * @author: 夏沫止水
 * @createTime: 2020-06-27 17:10
 **/

@FeignClient("member")
public interface MemberFeignService {

    @PostMapping(value = "/member/member/register")
    R register(@RequestBody UserRegisterVo vo);


    @PostMapping(value = "/member/member/login")
    R login(@RequestBody UserLoginVo vo);

    @PostMapping(value = "/member/member/oauth2/login")
    R oauthLogin(@RequestBody SocialUser socialUser) throws Exception;

    @PostMapping(value = "/member/member/qq/login")
    R qqLogin(QQUserInfo qqUserInfo);

    /**
     * 邮箱注册。
     *
     * <p>和上面的 {@link #register} 打的是同一个 member 接口（{@code /member/member/register}），
     * 只是请求体类型不同：member 那边收的是它的 MemberUserRegisterVo，只认
     * userName / password / email，这里多带的 password2 / code / agreement 会被忽略
     * （Spring Boot 默认关掉了 FAIL_ON_UNKNOWN_PROPERTIES）。</p>
     */
    @PostMapping(value = "/member/member/register")
    R emailRegister(@RequestBody UserEmailRegisterVo vo);

    /**
     * 邮箱 + 验证码登录：按邮箱取会员。
     *
     * <p>验证码由 auth 侧校验（存 Redis），member 不参与。
     * 这里用 {@code @RequestParam} 而不是请求体，是因为只需要一个邮箱，
     * 没必要为了它再定义一个 VO。</p>
     */
    @PostMapping(value = "/member/member/email/login")
    R emailLogin(@RequestParam("email") String email);
}

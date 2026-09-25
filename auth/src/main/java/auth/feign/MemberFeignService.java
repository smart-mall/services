package auth.feign;


import auth.vo.QQUserInfo;
import auth.vo.SocialUser;
import auth.vo.UserAccountVo;
import common.to.LoginLogTo;
import common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

    /**
     * 账号密码注册。
     *
     * <p>直接把 auth 的 {@link UserAccountVo} 作为请求体发过去，member 那边收的是它的
     * {@code MemberUserRegisterVo}，两边字段名必须都是 {@code username} / {@code password}
     * —— Jackson 按名字匹配，对不上就是 null。</p>
     *
     * <p>账号重复时 member 返回 15001。</p>
     */
    @PostMapping(value = "/member/member/register")
    R accountRegister(@RequestBody UserAccountVo vo);


    /** 账号密码登录。账号不存在或密码不对都是 15003 */
    @PostMapping(value = "/member/member/login")
    R accountLogin(@RequestBody UserAccountVo vo);

    @PostMapping(value = "/member/member/oauth2/login")
    R oauthLogin(@RequestBody SocialUser socialUser) throws Exception;

    @PostMapping(value = "/member/member/qq/login")
    R qqLogin(QQUserInfo qqUserInfo);

    /**
     * 邮箱验证码登录：按邮箱取会员，取不到就用 {@code username} 新建一个。
     *
     * <p>验证码由 auth 侧校验（存 Redis），member 不参与。所以这里只传账号和邮箱两个参数，
     * 用 {@code @RequestParam} 就够了，没必要为了它再定义一个 VO。</p>
     *
     * <p>需要新建账号但账号已被占用时返回 15001。</p>
     */
    @PostMapping(value = "/member/member/email/login")
    R emailLogin(@RequestParam("username") String username, @RequestParam("email") String email);

    /**
     * 手机验证码登录，语义同 {@link #emailLogin}，把邮箱换成手机号。
     */
    @PostMapping(value = "/member/member/mobile/login")
    R mobileLogin(@RequestParam("username") String username, @RequestParam("mobile") String mobile);

    /**
     * 按 id 取完整会员信息，给 {@code UserController} 用。
     *
     * <p><b>注意返回的键是 {@code member} 而不是 {@code data}</b> ——
     * member 的 {@code /info/{id}} 是代码生成器产出的 {@code R.ok().put("member", ...)}。
     * 取的时候别用错键，错了拿到的是 null 而不是报错。</p>
     */
    @GetMapping(value = "/member/member/info/{id}")
    R getUserInfo(@PathVariable("id") Long id);

    /**
     * 落一条登录记录。登录成功后由 {@code LoginLogService} 调用，失败不影响登录。
     */
    @PostMapping(value = "/member/memberloginlog/record")
    R recordLoginLog(@RequestBody LoginLogTo to);

    /**
     * 换绑手机号。号码已被别人绑定时 member 返回 15006。
     *
     * <p>改的是哪个账号，由 FeignConfig 自动转发的 {@code X-Member-Claims} 决定，所以不传 memberId
     * —— 传了就等于让浏览器也能指定改谁。验证码由 auth 校验（存 Redis），member 不参与。</p>
     */
    @PutMapping(value = "/member/member/mobile/update")
    R changeMobile(@RequestParam("mobile") String mobile);

    /** 换绑邮箱，占用时返回 15007 */
    @PutMapping(value = "/member/member/email/update")
    R changeEmail(@RequestParam("email") String email);
}

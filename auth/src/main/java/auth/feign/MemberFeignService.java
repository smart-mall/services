package auth.feign;


import auth.vo.QQUserInfo;
import auth.vo.SocialUser;
import auth.vo.UserAccountVo;
import common.to.LoginLogTo;
import common.utils.R;
import common.vo.MemberResponseVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;


/**
 * member 服务的 Feign 客户端：账号注册与登录、社交登录、换绑手机号 / 邮箱、登录记录写入。
 *
 * <p>注解里的路径与参数名由 member 侧决定，改这里必须同步改 member，否则请求参数对不上。
 */
@FeignClient("member")
public interface MemberFeignService {

    /**
     * 账号密码注册。
     *
     * <p>直接把 auth 的 {@link UserAccountVo} 作为请求体发给 member，对方收的是它的
     * {@code MemberUserRegisterVo}，两边字段名必须都是 {@code username} / {@code password}
     * —— Jackson 按名字匹配，对不上就是 null；账号重复时 member 返回 15001。</p>
     *
     * @param vo 注册入参，字段名须与 member 侧一致
     * @return 成功返回 {@code code:0}；账号重复时为 15001
     */
    @PostMapping(value = "/member/member/register")
    R<Void> accountRegister(@RequestBody UserAccountVo vo);


    /**
     * 账号密码登录。
     *
     * @param vo 登录入参，字段名须与 member 侧一致
     * @return 成功时 {@code data} 为会员信息；账号不存在或密码不对都是 15003
     */
    @PostMapping(value = "/member/member/login")
    R<MemberResponseVo> accountLogin(@RequestBody UserAccountVo vo);

    /**
     * 微博社交登录：member 按 {@code uid} 取会员，取不到就自动注册。
     *
     * @param socialUser 微博返回的令牌与用户标识
     * @return 成功时 {@code data} 为会员信息
     * @throws Exception 远程调用失败时抛出
     */
    @PostMapping(value = "/member/member/oauth2/login")
    R<MemberResponseVo> oauthLogin(@RequestBody SocialUser socialUser) throws Exception;

    /**
     * QQ 社交登录：member 按 {@code openId} 取会员，取不到就自动注册。
     *
     * @param qqUserInfo QQ 用户信息，{@code openId} 不能为空
     * @return 成功时 {@code data} 为会员信息
     */
    @PostMapping(value = "/member/member/qq/login")
    R<MemberResponseVo> qqLogin(QQUserInfo qqUserInfo);

    /**
     * 邮箱验证码登录：按邮箱取会员，取不到就用 {@code username} 新建一个。
     *
     * <p>验证码由 auth 侧校验（存 Redis），member 不参与，所以只传账号和邮箱两个参数；
     * 需要新建账号但账号已被占用时返回 15001。</p>
     *
     * @param username 新建账号时使用的用户名
     * @param email    收码邮箱，也是识别账号的依据
     * @return 成功时 {@code data} 为会员信息；账号被占用时为 15001
     */
    @PostMapping(value = "/member/member/email/login")
    R<MemberResponseVo> emailLogin(@RequestParam("username") String username, @RequestParam("email") String email);

    /**
     * 手机验证码登录，语义同 {@link #emailLogin}，把邮箱换成手机号。
     *
     * @param username 新建账号时使用的用户名
     * @param mobile   收码手机号，也是识别账号的依据
     * @return 成功时 {@code data} 为会员信息；账号被占用时为 15001
     */
    @PostMapping(value = "/member/member/mobile/login")
    R<MemberResponseVo> mobileLogin(@RequestParam("username") String username, @RequestParam("mobile") String mobile);

    /**
     * 按 id 取完整会员信息，给 {@code UserController} 用。
     *
     * <p>member 的 {@code /info/{id}} 是代码生成器产出的 {@code R.ok(member)}，会员数据仍在
     * {@code data} 键下，用 {@link R#getData()} 取；取不到时拿到的是 null 而不是报错。</p>
     *
     * @param id 会员 ID
     * @return 成功时 {@code data} 为会员信息
     */
    @GetMapping(value = "/member/member/info/{id}")
    R<MemberResponseVo> getUserInfo(@PathVariable("id") Long id);

    /**
     * 落一条登录记录。
     *
     * <p>登录成功后由 {@code LoginLogService} 调用，失败不影响登录。</p>
     *
     * @param to 登录记录，含会员 ID、IP、城市与登录类型
     * @return 成功返回 {@code code:0}
     */
    @PostMapping(value = "/member/memberloginlog/record")
    R<Void> recordLoginLog(@RequestBody LoginLogTo to);

    /**
     * 换绑手机号。
     *
     * <p>改的是哪个账号由 FeignConfig 自动转发的 {@code X-Member-Claims} 决定，所以不传 memberId
     * —— 传了就等于让浏览器也能指定改谁；验证码由 auth 校验（存 Redis），member 不参与。</p>
     *
     * @param mobile 新手机号
     * @return 成功返回 {@code code:0}；号码已被别人绑定时返回 15006
     */
    @PutMapping(value = "/member/member/mobile/update")
    R<Void> changeMobile(@RequestParam("mobile") String mobile);

    /**
     * 换绑邮箱，语义同 {@link #changeMobile}。
     *
     * @param email 新邮箱
     * @return 成功返回 {@code code:0}；邮箱已被别人绑定时返回 15007
     */
    @PutMapping(value = "/member/member/email/update")
    R<Void> changeEmail(@RequestParam("email") String email);
}

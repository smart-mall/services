package member.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import common.exception.BaseCodeEnum;
import common.utils.LoginUserUtils;
import common.vo.PageVO;
import common.utils.R;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import member.entity.MemberEntity;
import member.exception.UsernameException;
import member.service.MemberService;
import member.vo.MemberUserLoginVo;
import member.vo.MemberUserRegisterVo;
import member.vo.QQUserInfo;
import member.vo.SocialUser;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Supplier;



import common.query.PageQuery;
/**
 * 会员
 */
@Slf4j
@RestController
@RequestMapping("member/member")
public class MemberController {
    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    /**
     * 账号密码注册。只有账号和密码，手机号/邮箱不在这条链路里。
     */
    @PostMapping(value = "/register")
    public R<Void> register(@RequestBody MemberUserRegisterVo vo) {

        try {
            memberService.accountRegister(vo);
        } catch (UsernameException e) {
            return R.error(BaseCodeEnum.USER_EXIST_EXCEPTION);
        }

        return R.ok();
    }


    /**
     * 账号密码登录。只按 username 查，不再把手机号当账号（{@code username = ? OR mobile = ?} 已去掉）。
     */
    @PostMapping(value = "/login")
    public R<MemberEntity> login(@RequestBody MemberUserLoginVo vo) {

        MemberEntity memberEntity = memberService.loginByUsername(vo.getUsername(), vo.getPassword());

        if (memberEntity != null) {
            return R.ok(memberEntity);
        } else {
            return R.error(BaseCodeEnum.USERNAME_PASSWORD_EXCEPTION);
        }
    }


    /**
     * 邮箱验证码登录。
     *
     * <p>验证码是 auth 侧校验的（存在 Redis 里），这里只负责按邮箱找人；
     * 找不到就用 {@code username} 建一个新账号（自动注册），所以这个接口既是登录也是注册。
     * 账号被占用时返回 15001。</p>
     */
    @PostMapping(value = "/email/login")
    public R<MemberEntity> emailLogin(@RequestParam("username") String username,
                        @RequestParam("email") String email) {
        return loginOrRegister(() -> memberService.loginOrRegisterByEmail(username, email));
    }


    /**
     * 手机验证码登录，语义同 {@link #emailLogin}，把邮箱换成手机号。
     */
    @PostMapping(value = "/mobile/login")
    public R<MemberEntity> mobileLogin(@RequestParam("username") String username,
                         @RequestParam("mobile") String mobile) {
        return loginOrRegister(() -> memberService.loginOrRegisterByMobile(username, mobile));
    }


    /** 两条验证码链路共用的收尾：新建时账号撞了就转 15001，其余直接返回会员 */
    private R<MemberEntity> loginOrRegister(Supplier<MemberEntity> action) {
        try {
            return R.ok(action.get());
        } catch (UsernameException e) {
            return R.error(BaseCodeEnum.USER_EXIST_EXCEPTION);
        }
    }


    @PostMapping(value = "/oauth2/login")
    public R<MemberEntity> oauthLogin(@RequestBody SocialUser socialUser) throws Exception {

        MemberEntity memberEntity = memberService.login(socialUser);

        if (memberEntity != null) {
            return R.ok(memberEntity);
        } else {
            return R.error(BaseCodeEnum.USERNAME_PASSWORD_EXCEPTION);
        }
    }

    @PostMapping(value = "/qq/login")
    public R<MemberEntity> qqLogin(@RequestBody QQUserInfo qqUserInfo) {
        log.info("进入qq登录: {}", JSON.toJSONString(qqUserInfo, SerializerFeature.PrettyFormat));

        MemberEntity memberEntity = memberService.login(qqUserInfo);
        if (memberEntity != null) {
            return R.ok(memberEntity);
        } else {
            return R.error(BaseCodeEnum.USERNAME_PASSWORD_EXCEPTION);
        }
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R<PageVO<MemberEntity>> list(PageQuery query){
        PageVO<MemberEntity> page = memberService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 信息。
     *
     * <p>注意返回的键是 {@code member} 而不是 {@code data}（这是代码生成器留下的习惯），
     * auth 的 UserController 取完整用户信息走的就是这个接口，别取错了键。</p>
     */
    @RequestMapping("/info/{id}")
    public R<MemberEntity> info(@PathVariable("id") Long id){
		MemberEntity member = memberService.getById(id);

        return R.ok(member);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody MemberEntity member){
		memberService.save(member);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody MemberEntity member){
		memberService.updateById(member);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] ids){
		memberService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }


    /** 换绑手机号，由 auth 在验证码校验通过后调用；号码已被别人绑定返回 15006 */
    @PutMapping("/mobile/update")
    public R<Void> changeMobile(@RequestParam("mobile") String mobile, HttpServletRequest request) {
        memberService.changeMobile(LoginUserUtils.requireCurrentUser(request).getId(), mobile);
        return R.ok();
    }


    /** 换绑邮箱，语义同 {@link #changeMobile}；被占用返回 15007 */
    @PutMapping("/email/update")
    public R<Void> changeEmail(@RequestParam("email") String email, HttpServletRequest request) {
        memberService.changeEmail(LoginUserUtils.requireCurrentUser(request).getId(), email);
        return R.ok();
    }

}

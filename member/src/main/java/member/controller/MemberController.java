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
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;



import common.query.KeyPageQuery;
/**
 * 会员账号接口：账号密码注册与登录、验证码与社交登录、换绑手机号 / 邮箱，以及会员的后台 CRUD。
 *
 * <p>注册与各登录接口由 auth 经 Feign 调用，调用方没有登录态；换绑接口的会员 id 取自请求头
 * {@code X-Member-Claims}。路径不在 {@code /front} 下，经网关访问时按管理端接口鉴权。
 */
@Slf4j
@RestController
@RequestMapping("member/member")
public class MemberController {
    private final MemberService memberService;

    /**
     * 构造控制器，依赖由容器注入，创建后即可使用。
     *
     * @param memberService 会员业务服务
     */
    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    /**
     * 按账号密码注册会员，手机号与邮箱不在这条链路上。
     *
     * @param vo 注册入参，含账号与密码
     * @return 成功返回 {@code code:0}；账号已被占用时返回 15001
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
     * 按账号密码登录会员。
     *
     * <p>只按 {@code username} 匹配账号，手机号不能当作账号使用；账号不存在、没设过密码、
     * 密码不对三种情况统一返回 15003，不区分是其中哪一种。
     *
     * @param vo 登录入参，含账号与密码
     * @return 成功时 {@code data} 为会员信息；账号不存在或密码不对返回 15003
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
     * 邮箱验证码登录：按邮箱取会员，取不到就用 {@code username} 新建账号。
     *
     * <p>验证码由 auth 校验（存 Redis），本接口不参与；查不到就建号，所以它同时是登录与注册。
     *
     * @param username 新建账号时使用的用户名，已有账号时忽略
     * @param email 收码邮箱，也是识别账号的依据
     * @return 成功时 {@code data} 为会员信息；新建时账号被占用返回 15001
     */
    @PostMapping(value = "/email/login")
    public R<MemberEntity> emailLogin(@RequestParam("username") String username,
                        @RequestParam("email") String email) {
        return loginOrRegister(() -> memberService.loginOrRegisterByEmail(username, email));
    }


    /**
     * 手机验证码登录，语义同 {@link #emailLogin}，把邮箱换成手机号。
     *
     * @param username 新建账号时使用的用户名，已有账号时忽略
     * @param mobile 收码手机号，也是识别账号的依据
     * @return 成功时 {@code data} 为会员信息；新建时账号被占用返回 15001
     */
    @PostMapping(value = "/mobile/login")
    public R<MemberEntity> mobileLogin(@RequestParam("username") String username,
                         @RequestParam("mobile") String mobile) {
        return loginOrRegister(() -> memberService.loginOrRegisterByMobile(username, mobile));
    }


    /**
     * 两条验证码登录链路共用的收尾：新建账号撞名时转成 15001，其余原样返回会员。
     *
     * @param action 实际的登录或注册动作
     * @return 成功时 {@code data} 为会员信息；账号被占用时为 15001
     */
    private R<MemberEntity> loginOrRegister(Supplier<MemberEntity> action) {
        try {
            return R.ok(action.get());
        } catch (UsernameException e) {
            return R.error(BaseCodeEnum.USER_EXIST_EXCEPTION);
        }
    }


    /**
     * 微博社交登录：按 {@code uid} 取会员，取不到就自动注册。
     *
     * @param socialUser 微博返回的令牌与用户标识
     * @return 成功时 {@code data} 为会员信息；服务返回空时按 15003 处理
     * @throws Exception 调用微博接口失败时抛出
     */
    @PostMapping(value = "/oauth2/login")
    public R<MemberEntity> oauthLogin(@RequestBody SocialUser socialUser) throws Exception {

        MemberEntity memberEntity = memberService.login(socialUser);

        if (memberEntity != null) {
            return R.ok(memberEntity);
        } else {
            return R.error(BaseCodeEnum.USERNAME_PASSWORD_EXCEPTION);
        }
    }

    /**
     * QQ 社交登录：按 {@code openId} 取会员，取不到就自动注册。
     *
     * @param qqUserInfo QQ 用户信息，{@code openId} 不能为空
     * @return 成功时 {@code data} 为会员信息；服务返回空时按 15003 处理
     */
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
     * 分页查询会员，{@code key} 同时模糊匹配账号、昵称、手机号与邮箱。
     *
     * @param query 分页参数与关键字，{@code key} 不传则返回全部会员
     * @return 分页结果，{@code rows} 为会员列表
     */
    @RequestMapping("/list")
    public R<PageVO<MemberEntity>> list(KeyPageQuery query){
        PageVO<MemberEntity> page = memberService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 按会员 ID 批量查询昵称。
     *
     * <p>给只存了会员 ID 的跨服务记录补一个展示用的名字，由 coupon 经 Feign 直连本服务调用；
     * 直连不走网关，因此不需要管理端凭证。
     *
     * @param memberIds 会员 ID 列表
     * @return 会员 ID 到昵称的映射；入参里查不到或昵称为空的 ID 不会出现在结果中
     */
    @PostMapping("/getMemberNames")
    public R<Map<Long, String>> getMemberNames(@RequestBody List<Long> memberIds){
        return R.ok(memberService.getMemberNames(memberIds));
    }


    /**
     * 按主键查询单个会员。
     *
     * <p>auth 的 {@code UserController} 取完整用户信息走的就是这个接口，会员数据在 {@code R.data} 下；
     * id 不存在时 {@code data} 为 {@code null}，不报错。
     *
     * @param id 会员 ID
     * @return 会员信息；id 不存在时 {@code data} 为 {@code null}
     */
    @RequestMapping("/info/{id}")
    public R<MemberEntity> info(@PathVariable("id") Long id){
		MemberEntity member = memberService.getById(id);

        return R.ok(member);
    }

    /**
     * 新增一个会员。
     *
     * @param member 会员内容，主键由数据库生成
     * @return 统一成功响应，不含业务数据
     */
    // TODO: 整个后台会员 CRUD 是代码生成器产物，后端没有"后台建会员"这个需求（会员走注册链路），
    //       应整块删除；在此之前不要在这里补业务逻辑 —— 它落库明文密码是常态，不算要修的缺陷
    @RequestMapping("/save")
    public R<Void> save(@RequestBody MemberEntity member){
		memberService.save(member);

        return R.ok();
    }

    /**
     * 按主键修改一个会员。
     *
     * <p>密码传空视为"不改密码"：实体上的密码是 {@code WRITE_ONLY}，后台编辑弹窗读不到哈希、
     * 输入框是空的，保存时会把空串传回来；直接落库会把密码覆盖成空，账号从此登不上。</p>
     *
     * @param member 会员内容，主键必填；为 {@code null} 的字段不参与更新
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody MemberEntity member){
		if (!StringUtils.hasText(member.getPassword())) {
			member.setPassword(null);
		}
		memberService.updateById(member);

        return R.ok();
    }

    /**
     * 按主键批量删除会员。
     *
     * @param ids 待删除的会员主键数组
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] ids){
		memberService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }


    /**
     * 换绑当前登录会员的手机号，由 auth 在验证码校验通过后调用。
     *
     * <p>会员 id 只取自请求头 {@code X-Member-Claims}，不接受调用方指定。
     *
     * @param mobile 新手机号
     * @param request 当前请求，会员身份从它携带的 {@code X-Member-Claims} 头解析
     * @return 成功返回 {@code code:0}；号码已被别人绑定返回 15006
     */
    @PutMapping("/mobile/update")
    public R<Void> changeMobile(@RequestParam("mobile") String mobile, HttpServletRequest request) {
        memberService.changeMobile(LoginUserUtils.requireCurrentUser(request).getId(), mobile);
        return R.ok();
    }


    /**
     * 换绑当前登录会员的邮箱，语义同 {@link #changeMobile}。
     *
     * @param email 新邮箱
     * @param request 当前请求，会员身份从它携带的 {@code X-Member-Claims} 头解析
     * @return 成功返回 {@code code:0}；邮箱已被别人绑定返回 15007
     */
    @PutMapping("/email/update")
    public R<Void> changeEmail(@RequestParam("email") String email, HttpServletRequest request) {
        memberService.changeEmail(LoginUserUtils.requireCurrentUser(request).getId(), email);
        return R.ok();
    }

}

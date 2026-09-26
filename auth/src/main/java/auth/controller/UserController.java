package auth.controller;

import auth.feign.MemberFeignService;
import auth.utils.VerifyCodeUtils;
import auth.vo.EmailChangeVo;
import auth.vo.MobileChangeVo;
import common.constant.AuthServerConstant;
import common.exception.BaseCodeEnum;
import common.exception.BaseException;
import common.exception.ValidationException;
import common.utils.LoginUserUtils;
import common.utils.R;
import common.vo.MemberResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 当前用户的账号信息：读完整资料、换绑手机号 / 邮箱。登录态由网关校验后经 {@code X-Member-Claims} 注入，
 * 这里只读那个头。换绑放在 auth 是因为验证码的生成、防刷、一次性消费都收在 {@link VerifyCodeUtils} 里。
 */
@Slf4j
@RestController
@RequestMapping("auth/front/jwt/user")
public class UserController {

    private final MemberFeignService memberFeignService;
    private final StringRedisTemplate stringRedisTemplate;

    public UserController(MemberFeignService memberFeignService,
                          StringRedisTemplate stringRedisTemplate) {
        this.memberFeignService = memberFeignService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /** 当前登录用户的完整信息 */
    @GetMapping("/info")
    public R<MemberResponseVo> info(HttpServletRequest request) {
        Long memberId = LoginUserUtils.requireCurrentUser(request).getId();

        R<MemberResponseVo> memberR = memberFeignService.getUserInfo(memberId);
        if (memberR.getCode() != 0) {
            log.warn("查询用户信息失败: memberId={}, code={}, msg={}",
                    memberId, memberR.getCode(), memberR.getMsg());
            return R.error(memberR.getCode(), memberR.getMsg());
        }

        // 注意键是 member 不是 data：member 的 /info/{id} 是代码生成器产出的 R.ok(...)
        MemberResponseVo user = memberR.getData();
        if (user == null) {
            // 签出来的 token 有效但库里没人（账号被删了），等同于未登录
            log.warn("token 有效但会员不存在: memberId={}", memberId);
            throw new BaseException(BaseCodeEnum.NOT_LOGIN_EXCEPTION);
        }

        // 和 AbstractLoginController#issueToken 一样：这两个字段不能出这个接口
        user.setPassword(null);
        user.setAccessToken(null);

        return R.ok(user);
    }

    /** 换绑手机号。验证码只读不删，换绑成功才消费（失败时能拿同一个码重试） */
    @PutMapping("/mobile")
    public R<Void> changeMobile(@Valid @RequestBody MobileChangeVo vo) {

        if (!VerifyCodeUtils.verify(stringRedisTemplate, AuthServerConstant.SMS_CODE_CACHE_PREFIX,
                vo.getMobile(), vo.getCode())) {
            throw new ValidationException("code", "验证码错误");
        }

        R<Void> r = memberFeignService.changeMobile(vo.getMobile());
        if (r.getCode() != 0) {
            return R.error(r.getCode(), r.getMsg());
        }

        VerifyCodeUtils.consume(stringRedisTemplate, AuthServerConstant.SMS_CODE_CACHE_PREFIX, vo.getMobile());
        return R.ok();
    }

    /** 换绑邮箱，语义同 {@link #changeMobile}，占用时返回 15007 */
    @PutMapping("/email")
    public R<Void> changeEmail(@Valid @RequestBody EmailChangeVo vo) {

        if (!VerifyCodeUtils.verify(stringRedisTemplate, AuthServerConstant.EMAIL_CODE_CACHE_PREFIX,
                vo.getEmail(), vo.getCode())) {
            throw new ValidationException("code", "验证码错误");
        }

        R<Void> r = memberFeignService.changeEmail(vo.getEmail());
        if (r.getCode() != 0) {
            return R.error(r.getCode(), r.getMsg());
        }

        VerifyCodeUtils.consume(stringRedisTemplate, AuthServerConstant.EMAIL_CODE_CACHE_PREFIX, vo.getEmail());
        return R.ok();
    }
}

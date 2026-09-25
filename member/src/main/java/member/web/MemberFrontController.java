package member.web;

import common.exception.BaseCodeEnum;
import common.exception.BaseException;
import common.utils.JwtUtils;
import common.utils.LoginUserUtils;
import common.utils.R;
import common.vo.MemberResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import member.entity.MemberEntity;
import member.entity.MemberReceiveAddressEntity;
import member.service.MemberReceiveAddressService;
import member.service.MemberService;
import member.vo.AddressSaveVo;
import member.vo.MemberProfileUpdateVo;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 会员中心的前台接口。
 *
 * <p>路径前缀 {@code member/front} 配合网关的 {@code Path=/api/member/**} 路由
 * （重写规则剥掉 {@code /api}），所以 SPA 写 {@code /api/member/front/profile}。</p>
 *
 * <p><b>会员 id 只取自网关注入的 {@code X-Member-Claims}，不接受前端传参。</b>
 * 传参的话就成了"改任意会员的资料/地址"。member 没有拦截器，所以这里每个入口
 * 自己判一次未登录（HTTP 401 + JSON，和 cart / order 的拦截器语义一致）。</p>
 *
 * <p><b>为什么修改资料要返回新 token</b>：JWT 里带了 nickname 和 header，改完不重签的话
 * token 里还是旧值。返回的形状和 auth 的登录接口完全一致（{@code {token, expiresIn, user}}），
 * 前端可以直接用 store 的同一个 applyLogin 处理。</p>
 */
@RestController
@RequestMapping("member/front")
public class MemberFrontController {

    private final MemberService memberService;
    private final MemberReceiveAddressService memberReceiveAddressService;
    private final JwtUtils jwtUtils;

    public MemberFrontController(MemberService memberService,
                                 MemberReceiveAddressService memberReceiveAddressService,
                                 JwtUtils jwtUtils) {
        this.memberService = memberService;
        this.memberReceiveAddressService = memberReceiveAddressService;
        this.jwtUtils = jwtUtils;
    }

    /* ═══════════════════ 资料 ═══════════════════ */

    /**
     * 修改资料。只接受白名单里的 7 个字段，整体替换语义。
     *
     * <p>成功返回新的 token —— 昵称和头像在 JWT 里，不重签的话头部要等重新登录才会变。</p>
     */
    @PutMapping("/profile")
    public R updateProfile(@Valid @RequestBody MemberProfileUpdateVo vo,
                           HttpServletRequest request,
                           HttpServletResponse response) throws IOException {
        Long memberId = currentMemberId(request, response);
        if (memberId == null) {
            return null;
        }

        return withFreshToken(memberService.updateProfile(memberId, vo));
    }

    /* ═══════════════════ 收货地址 ═══════════════════ */

    /** 我的收货地址，默认地址排在最前 */
    @GetMapping("/address")
    public R listAddress(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Long memberId = currentMemberId(request, response);
        if (memberId == null) {
            return null;
        }

        List<MemberReceiveAddressEntity> addresses = memberReceiveAddressService.listMine(memberId);
        return R.ok().setData(addresses);
    }

    /** 新增地址。第一条自动成为默认 */
    @PostMapping("/address")
    public R createAddress(@Valid @RequestBody AddressSaveVo vo,
                           HttpServletRequest request,
                           HttpServletResponse response) throws IOException {
        Long memberId = currentMemberId(request, response);
        if (memberId == null) {
            return null;
        }

        return R.ok().setData(memberReceiveAddressService.create(memberId, vo));
    }

    /** 修改地址 */
    @PutMapping("/address/{id}")
    public R updateAddress(@PathVariable("id") Long id,
                           @Valid @RequestBody AddressSaveVo vo,
                           HttpServletRequest request,
                           HttpServletResponse response) throws IOException {
        Long memberId = currentMemberId(request, response);
        if (memberId == null) {
            return null;
        }

        return R.ok().setData(memberReceiveAddressService.update(memberId, id, vo));
    }

    /** 删除地址 */
    @DeleteMapping("/address/{id}")
    public R deleteAddress(@PathVariable("id") Long id,
                           HttpServletRequest request,
                           HttpServletResponse response) throws IOException {
        Long memberId = currentMemberId(request, response);
        if (memberId == null) {
            return null;
        }

        memberReceiveAddressService.delete(memberId, id);
        return R.ok();
    }

    /** 设为默认地址 */
    @PutMapping("/address/{id}/default")
    public R setDefaultAddress(@PathVariable("id") Long id,
                               HttpServletRequest request,
                               HttpServletResponse response) throws IOException {
        Long memberId = currentMemberId(request, response);
        if (memberId == null) {
            return null;
        }

        memberReceiveAddressService.setDefault(memberId, id);
        return R.ok();
    }

    /* ═══════════════════ 内部 ═══════════════════ */

    /** 当前登录会员 id；未登录时写好 401 + JSON 并返回 null */
    private Long currentMemberId(HttpServletRequest request, HttpServletResponse response) throws IOException {
        MemberResponseVo user = LoginUserUtils.currentUser(request);
        if (user == null || user.getId() == null) {
            LoginUserUtils.writeUnauthorized(response);
            return null;
        }
        return user.getId();
    }

    /**
     * 拼登录态的响应：{@code {code:0, data:{token, expiresIn, user}}}。
     *
     * <p>形状必须和 auth 的登录接口一致，前端 store 的 applyLogin 直接吃它。
     * 顺带把 password / accessToken 置空 —— 它们不能出任何接口。</p>
     */
    private R withFreshToken(MemberEntity member) {
        MemberResponseVo user = new MemberResponseVo();
        BeanUtils.copyProperties(member, user);
        user.setPassword(null);
        user.setAccessToken(null);

        Map<String, Object> data = new HashMap<>();
        data.put("token", jwtUtils.create(user));
        data.put("expiresIn", jwtUtils.getExpireSeconds());
        data.put("user", user);
        return R.ok().setData(data);
    }
}

package member.web;

import common.utils.JwtUtils;
import common.utils.LoginUserUtils;
import common.utils.R;
import common.vo.MemberResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import member.entity.MemberEntity;
import member.entity.MemberReceiveAddressEntity;
import member.service.MemberLoginLogService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import common.vo.PageVO;
import member.entity.MemberLoginLogEntity;
/**
 * 会员中心的前台接口，全部要求登录；会员 id 只取自 {@code X-Member-Claims}，不接受前端传参。
 * 改资料会重签 token，返回形状和 auth 的登录接口一致。
 */
@RestController
@RequestMapping("member/front/jwt")
public class MemberFrontController {

    private final MemberService memberService;
    private final MemberReceiveAddressService memberReceiveAddressService;
    private final MemberLoginLogService memberLoginLogService;
    private final JwtUtils jwtUtils;

    public MemberFrontController(MemberService memberService,
                                 MemberReceiveAddressService memberReceiveAddressService,
                                 MemberLoginLogService memberLoginLogService,
                                 JwtUtils jwtUtils) {
        this.memberService = memberService;
        this.memberReceiveAddressService = memberReceiveAddressService;
        this.memberLoginLogService = memberLoginLogService;
        this.jwtUtils = jwtUtils;
    }

    /* ═══════════════════ 资料 ═══════════════════ */

    /** 修改资料。只接受白名单里的 7 个字段，整体替换语义；成功返回新 token */
    @PutMapping("/profile")
    public R<Map<String, Object>> updateProfile(@Valid @RequestBody MemberProfileUpdateVo vo, HttpServletRequest request) {
        return withFreshToken(memberService.updateProfile(LoginUserUtils.requireCurrentUser(request).getId(), vo));
    }

    /* ═══════════════════ 收货地址 ═══════════════════ */

    /** 我的收货地址，默认地址排在最前 */
    @GetMapping("/address")
    public R<List<MemberReceiveAddressEntity>> listAddress(HttpServletRequest request) {
        List<MemberReceiveAddressEntity> addresses = memberReceiveAddressService.listMine(LoginUserUtils.requireCurrentUser(request).getId());
        return R.ok(addresses);
    }

    /** 新增地址。第一条自动成为默认 */
    @PostMapping("/address")
    public R<MemberReceiveAddressEntity> createAddress(@Valid @RequestBody AddressSaveVo vo, HttpServletRequest request) {
        return R.ok(memberReceiveAddressService.create(LoginUserUtils.requireCurrentUser(request).getId(), vo));
    }

    /** 修改地址 */
    @PutMapping("/address/{id}")
    public R<MemberReceiveAddressEntity> updateAddress(@PathVariable("id") Long id,
                           @Valid @RequestBody AddressSaveVo vo,
                           HttpServletRequest request) {
        return R.ok(memberReceiveAddressService.update(LoginUserUtils.requireCurrentUser(request).getId(), id, vo));
    }

    /** 删除地址 */
    @DeleteMapping("/address/{id}")
    public R<Void> deleteAddress(@PathVariable("id") Long id, HttpServletRequest request) {
        memberReceiveAddressService.delete(LoginUserUtils.requireCurrentUser(request).getId(), id);
        return R.ok();
    }

    /** 设为默认地址 */
    @PutMapping("/address/{id}/default")
    public R<Void> setDefaultAddress(@PathVariable("id") Long id, HttpServletRequest request) {
        memberReceiveAddressService.setDefault(LoginUserUtils.requireCurrentUser(request).getId(), id);
        return R.ok();
    }

    /* ═══════════════════ 登录记录 ═══════════════════ */

    /** 当前登录会员自己的登录记录，按时间倒序。参数 {@code pageNum / pageSize} */
    @GetMapping("/memberloginlog/mine")
    public R<PageVO<MemberLoginLogEntity>> listLoginLog(@RequestParam Map<String, Object> params, HttpServletRequest request) {
        return R.ok(memberLoginLogService.queryMine(LoginUserUtils.requireCurrentUser(request).getId(), params));
    }

    /* ═══════════════════ 内部 ═══════════════════ */

    /** 拼登录态响应：{@code {code:0, data:{token, expiresIn, user}}}，形状同 auth 的登录接口 */
    private R<Map<String, Object>> withFreshToken(MemberEntity member) {
        MemberResponseVo user = new MemberResponseVo();
        BeanUtils.copyProperties(member, user);
        user.setPassword(null);
        user.setAccessToken(null);

        Map<String, Object> data = new HashMap<>();
        data.put("token", jwtUtils.create(user));
        data.put("expiresIn", jwtUtils.getExpireSeconds());
        data.put("user", user);
        return R.ok(data);
    }
}

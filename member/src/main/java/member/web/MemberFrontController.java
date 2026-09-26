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
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import common.vo.PageVO;
import member.entity.MemberLoginLogEntity;
import common.query.PageQuery;
/**
 * 会员中心的前台接口：资料修改、收货地址管理、本人登录记录查询。
 *
 * <p>路径在 {@code /front/jwt} 下，会员 id 只取自网关注入的 {@code X-Member-Claims}，不接受前端传参；
 * 改资料会重签 token，返回形状与 auth 的登录接口一致。
 */
@RestController
@RequestMapping("member/front/jwt")
public class MemberFrontController {

    private final MemberService memberService;
    private final MemberReceiveAddressService memberReceiveAddressService;
    private final MemberLoginLogService memberLoginLogService;
    private final JwtUtils jwtUtils;

    /**
     * 构造控制器，依赖由容器注入，创建后即可使用。
     *
     * @param memberService 会员业务服务
     * @param memberReceiveAddressService 会员收货地址服务
     * @param memberLoginLogService 会员登录记录服务
     * @param jwtUtils 会员登录态签发工具
     */
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

    /**
     * 修改当前登录会员的资料，成功时返回重签的登录态。
     *
     * <p>只接受白名单里的 7 个字段，整体替换语义：没填的字段落成 {@code null}，不是"保持不变"。
     *
     * @param vo 资料入参，昵称不能为空
     * @param request 当前请求，会员身份从它携带的 {@code X-Member-Claims} 头解析
     * @return 登录态响应，{@code data} 含 token、expiresIn 与用户信息
     */
    @PutMapping("/profile")
    public R<Map<String, Object>> updateProfile(@Valid @RequestBody MemberProfileUpdateVo vo, HttpServletRequest request) {
        return withFreshToken(memberService.updateProfile(LoginUserUtils.requireCurrentUser(request).getId(), vo));
    }

    /* ═══════════════════ 收货地址 ═══════════════════ */

    /**
     * 查询当前登录会员的全部收货地址，默认地址排在最前。
     *
     * @param request 当前请求，会员身份从它携带的 {@code X-Member-Claims} 头解析
     * @return 地址列表；没有地址时返回空列表
     */
    @GetMapping("/address")
    public R<List<MemberReceiveAddressEntity>> listAddress(HttpServletRequest request) {
        List<MemberReceiveAddressEntity> addresses = memberReceiveAddressService.listMine(LoginUserUtils.requireCurrentUser(request).getId());
        return R.ok(addresses);
    }

    /**
     * 为当前登录会员新增收货地址。
     *
     * <p>第一条地址自动成为默认；条数超过上限时返回 15008。
     *
     * @param vo 地址入参，不含 memberId，归属由登录态决定
     * @param request 当前请求，会员身份从它携带的 {@code X-Member-Claims} 头解析
     * @return 落库后的地址，含生成的 id 与 defaultStatus
     */
    @PostMapping("/address")
    public R<MemberReceiveAddressEntity> createAddress(@Valid @RequestBody AddressSaveVo vo, HttpServletRequest request) {
        return R.ok(memberReceiveAddressService.create(LoginUserUtils.requireCurrentUser(request).getId(), vo));
    }

    /**
     * 修改当前登录会员名下的一条收货地址。
     *
     * <p>id 必须属于当前会员，不属于时按"地址不存在"返回 17004。
     *
     * @param id 地址 ID
     * @param vo 地址入参
     * @param request 当前请求，会员身份从它携带的 {@code X-Member-Claims} 头解析
     * @return 修改后的地址
     */
    @PutMapping("/address/{id}")
    public R<MemberReceiveAddressEntity> updateAddress(@PathVariable("id") Long id,
                           @Valid @RequestBody AddressSaveVo vo,
                           HttpServletRequest request) {
        return R.ok(memberReceiveAddressService.update(LoginUserUtils.requireCurrentUser(request).getId(), id, vo));
    }

    /**
     * 删除当前登录会员名下的一条收货地址。
     *
     * <p>删掉的是默认地址时，剩下最早的一条会被提为默认；id 不属于当前会员时返回 17004。
     *
     * @param id 地址 ID
     * @param request 当前请求，会员身份从它携带的 {@code X-Member-Claims} 头解析
     * @return 统一成功响应，不含业务数据
     */
    @DeleteMapping("/address/{id}")
    public R<Void> deleteAddress(@PathVariable("id") Long id, HttpServletRequest request) {
        memberReceiveAddressService.delete(LoginUserUtils.requireCurrentUser(request).getId(), id);
        return R.ok();
    }

    /**
     * 把当前登录会员的一条收货地址设为默认。
     *
     * <p>同一事务内清掉该会员其余地址的默认标记；id 不属于当前会员时返回 17004。
     *
     * @param id 地址 ID
     * @param request 当前请求，会员身份从它携带的 {@code X-Member-Claims} 头解析
     * @return 统一成功响应，不含业务数据
     */
    @PutMapping("/address/{id}/default")
    public R<Void> setDefaultAddress(@PathVariable("id") Long id, HttpServletRequest request) {
        memberReceiveAddressService.setDefault(LoginUserUtils.requireCurrentUser(request).getId(), id);
        return R.ok();
    }

    /* ═══════════════════ 登录记录 ═══════════════════ */

    /**
     * 分页查询当前登录会员自己的登录记录，按时间倒序。
     *
     * @param query 分页参数，{@code page} 为页码、{@code limit} 为每页条数
     * @param request 当前请求，会员身份从它携带的 {@code X-Member-Claims} 头解析
     * @return 分页结果，{@code rows} 为登录记录
     */
    @GetMapping("/memberloginlog/mine")
    public R<PageVO<MemberLoginLogEntity>> listLoginLog(PageQuery query, HttpServletRequest request) {
        return R.ok(memberLoginLogService.queryMine(LoginUserUtils.requireCurrentUser(request).getId(), query));
    }

    /* ═══════════════════ 内部 ═══════════════════ */

    /**
     * 拼装登录态响应：{@code {code:0, data:{token, expiresIn, user}}}，形状同 auth 的登录接口。
     *
     * @param member 会员信息，密码与 accessToken 会被抹掉后再放进 user
     * @return 登录态响应
     */
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

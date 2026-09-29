package coupon.controller;

import common.utils.LoginUserUtils;
import common.utils.R;
import common.vo.MemberResponseVo;
import common.vo.PageVO;
import coupon.entity.CouponHistoryEntity;
import coupon.service.CouponService;
import coupon.service.CouponUseService;
import coupon.vo.CouponHistoryPageQuery;
import coupon.vo.CouponReceivableVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 优惠券的会员端接口：券中心、领券与我的券。
 *
 * <p>路径在 {@code /front/jwt} 下，经网关访问时必须带会员凭证，网关校验通过后把身份写进
 * {@code X-Member-Claims} 头；本类只做参数接收与转发，业务规则都在 service 层。
 *
 * <p>会员身份一律取自请求头，不接受前端传 {@code memberId}。
 *
 * <p>没有"按购物车筛可用券"的接口：结算页展示的可用券由 order 的结算接口一并返回，
 * 那才是会员看到券列表的地方；再开一个入口会让两边各算一份抵扣额。
 *
 * <p>本类无状态、线程安全：两个字段都是构造器注入的 service，请求之间不共享可变状态。
 */
@RestController
@RequestMapping("coupon/front/jwt")
public class CouponFrontController {

    private final CouponService couponService;

    private final CouponUseService couponUseService;

    /**
     * 注入券模板服务与券使用服务。
     *
     * @param couponService 券模板服务，提供领券入口
     * @param couponUseService 券使用服务，提供我的券与可用券查询
     */
    public CouponFrontController(CouponService couponService, CouponUseService couponUseService) {
        this.couponService = couponService;
        this.couponUseService = couponUseService;
    }

    /**
     * 领取一张优惠券。
     *
     * <p>失败原因靠错误码区分：券不存在、当前不可领取、已领完、超出每人限领。
     *
     * @param request 当前请求，用于取出登录会员
     * @param couponId 优惠券主键
     * @return 统一成功响应，不含业务数据
     */
    @PostMapping("/receive/{couponId}")
    public R<Void> receive(HttpServletRequest request, @PathVariable("couponId") Long couponId) {
        MemberResponseVo user = LoginUserUtils.requireCurrentUser(request);
        couponService.receive(couponId, user);

        return R.ok();
    }

    /**
     * 列出当前会员可领取的券，供券中心的"可领取"一档展示。
     *
     * <p>每张券带剩余张数与该会员已领张数，前端据此决定按钮是"立即领取""已领取"还是"已领完"。
     *
     * @param request 当前请求，用于取出登录会员
     * @return 可领取的券列表，一张都没有时为空列表
     */
    @GetMapping("/receivable")
    public R<List<CouponReceivableVo>> receivable(HttpServletRequest request) {
        MemberResponseVo user = LoginUserUtils.requireCurrentUser(request);

        return R.ok(couponService.listReceivable(user.getId()));
    }

    /**
     * 分页查询当前会员的券。
     *
     * <p>每行都带券名、面额、门槛、有效期与使用状态文案，会员端不必再逐条回查券详情。
     *
     * @param request 当前请求，用于取出登录会员
     * @param query 分页与状态筛选条件，{@code useType} 不传时不限状态
     * @return 分页结果，{@code rows} 为领取记录
     */
    @GetMapping("/myCoupons")
    public R<PageVO<CouponHistoryEntity>> myCoupons(HttpServletRequest request, CouponHistoryPageQuery query) {
        MemberResponseVo user = LoginUserUtils.requireCurrentUser(request);

        return R.ok(couponUseService.queryMyCoupons(user.getId(), query));
    }

}

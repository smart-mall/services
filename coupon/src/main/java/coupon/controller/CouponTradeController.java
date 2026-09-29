package coupon.controller;

import common.to.CouponCartItemVo;
import common.to.CouponUsableVo;
import common.utils.R;
import coupon.service.CouponUseService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 优惠券的状态流转接口，供 order 在下单、支付、关单三个时机调用。
 *
 * <p>路径不在 {@code /front} 下，调用方是服务而不是浏览器：order 走
 * {@code @FeignClient("coupon")} 直连，不经网关，没有会员凭证可带，所以会员身份由调用方显式传
 * {@code memberId}，本类不做登录校验。
 *
 * <p>把 {@code memberId} 当成可信入参的前提是这些路径不会由浏览器直接触达。经网关进来的请求里
 * {@code /coupon/trade/**} 既不属于前台公开路径也不属于 {@code /front/jwt}，
 * 会按管理端接口要求管理端凭证，普通会员调不到。
 *
 * <p>购物项以裸 {@code List} 作为请求体：本类只收一个列表，再包一层对象没有信息量。
 * 逐项校验因此不在本层做，由 service 入口显式判。
 *
 * <p>本类无状态、线程安全：只持有一个构造器注入的 service。
 */
@RestController
@RequestMapping("coupon/trade")
public class CouponTradeController {

    private final CouponUseService couponUseService;

    /**
     * 注入券使用服务。
     *
     * @param couponUseService 券使用服务
     */
    public CouponTradeController(CouponUseService couponUseService) {
        this.couponUseService = couponUseService;
    }

    /**
     * 列出指定会员在给定购物车下可用的全部券。
     *
     * <p>结算页的券列表由 order 的结算接口经本接口取得：会员端的公开入口拿不到服务间调用所需的凭证，
     * 而身份在这里由 {@code memberId} 显式传入。
     *
     * @param memberId 持券会员主键
     * @param items 购物项列表，至少一项，元素不能缺少 skuId、单价或数量
     * @return 可用券列表，按抵扣额从大到小排；一张都没有时为空列表
     */
    @PostMapping("/available")
    public R<List<CouponUsableVo>> available(@RequestParam("memberId") Long memberId,
                                             @RequestBody List<CouponCartItemVo> items) {
        return R.ok(couponUseService.listUsable(memberId, items));
    }

    /**
     * 判定指定的一张券在给定购物车下能减多少。
     *
     * <p>下单算价与提交校验共用本接口，保证结算页展示的抵扣额与落库的抵扣额是同一个数。
     * 券不可用时返回具体原因（不在可使用期内、没有适用商品、未达到门槛），不降级成"不能用"。
     *
     * @param memberId 持券会员主键
     * @param couponHistoryId 指定的领取记录主键，必填
     * @param items 购物项列表，至少一项，元素不能缺少 skuId、单价或数量
     * @return 该券的抵扣结果，含适用范围内金额、实际抵扣额与范围内的 SKU
     */
    @PostMapping("/discount")
    public R<CouponUsableVo> discount(@RequestParam("memberId") Long memberId,
                                      @RequestParam("couponHistoryId") Long couponHistoryId,
                                      @RequestBody List<CouponCartItemVo> items) {
        return R.ok(couponUseService.computeDiscount(memberId, items, couponHistoryId));
    }

    /**
     * 下单占用一张券。
     *
     * <p>调用前必须已经用 {@code /discount} 判定过可用性：本接口只认状态，不重新比对购物车与门槛。
     *
     * @param couponHistoryId 领取记录主键
     * @param memberId 持券会员主键
     * @param orderId 订单主键
     * @param orderSn 订单号，核销与退回都靠它找回来
     * @return 统一成功响应，不含业务数据
     */
    @PostMapping("/lock")
    public R<Void> lock(@RequestParam("couponHistoryId") Long couponHistoryId,
                        @RequestParam("memberId") Long memberId,
                        @RequestParam("orderId") Long orderId,
                        @RequestParam("orderSn") String orderSn) {
        couponUseService.lock(couponHistoryId, memberId, orderId, orderSn);

        return R.ok();
    }

    /**
     * 付款核销一张券。
     *
     * <p>幂等：支付回调重复投递时，已经核销过的订单再次调用按成功处理。
     *
     * @param orderSn 订单号
     * @return 统一成功响应，不含业务数据
     */
    @PostMapping("/use")
    public R<Void> use(@RequestParam("orderSn") String orderSn) {
        couponUseService.use(orderSn);

        return R.ok();
    }

    /**
     * 取消或超时关单时退回一张券。
     *
     * <p>幂等且不报错：关单消息重投、以及本来就没用券的订单，都当作正常情况。
     *
     * @param orderSn 订单号
     * @return 统一成功响应，不含业务数据
     */
    @PostMapping("/unlock")
    public R<Void> unlock(@RequestParam("orderSn") String orderSn) {
        couponUseService.unlock(orderSn);

        return R.ok();
    }

}

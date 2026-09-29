package order.feign;

import common.to.CouponCartItemVo;
import common.to.CouponUsableVo;
import common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 优惠券服务的远程调用接口，覆盖券在本单上的算价、占用、核销与退回。
 *
 * <p>这几个接口都在 {@code /coupon/trade} 下，<b>不经网关</b>，没有会员凭证可带，
 * 所以会员身份由 {@code memberId} 显式传参而不是从请求头取。
 *
 * <p>返回值统一是 {@link R}：券服务把业务校验失败也包在 {@code code} 里返回，
 * 调用方必须先判 {@code code}，非 0 时把码与文案原样转成 {@link common.exception.BaseException}
 * 往上抛，会员才能看到"未达到使用门槛"这类具体原因，而不是笼统的下单失败。
 */
@FeignClient("coupon")
public interface CouponFeignService {

    /**
     * 列出指定会员在给定购物车下可用的全部券。
     *
     * <p>结算页的券列表由本接口取得，每张券都带自己的抵扣额。
     *
     * @param memberId 持券会员主键，不能为 {@code null}
     * @param items 购物项列表，不能为 {@code null} 或空集合
     * @return 统一响应；{@code code} 为 0 时 {@code data} 是按抵扣额从大到小排的可用券，
     *         一张都没有时为空列表；{@code code} 非 0 时 {@code data} 为 {@code null}
     */
    @PostMapping("/coupon/trade/available")
    R<List<CouponUsableVo>> available(@RequestParam("memberId") Long memberId,
                                      @RequestBody List<CouponCartItemVo> items);

    /**
     * 判定指定的一张券在给定购物车下能减多少。
     *
     * <p>结算页算价与提交订单校验共用本接口，两处算出的必须是同一个数。
     *
     * @param memberId 持券会员主键，不能为 {@code null}
     * @param couponHistoryId 指定的领取记录主键，不能为 {@code null}
     * @param items 购物项列表，不能为 {@code null} 或空集合
     * @return 统一响应；{@code code} 为 0 时 {@code data} 是抵扣结果，含总额与覆盖的 SKU；
     *         {@code code} 非 0 时 {@code data} 为 {@code null}，{@code msg} 是具体原因
     */
    @PostMapping("/coupon/trade/discount")
    R<CouponUsableVo> discount(@RequestParam("memberId") Long memberId,
                               @RequestParam("couponHistoryId") Long couponHistoryId,
                               @RequestBody List<CouponCartItemVo> items);

    /**
     * 下单占用一张券，把券从"未使用"置为"占用中"并记下订单。
     *
     * <p>调用前必须已经用 {@link #discount} 判定过可用性：本接口只认券的状态，
     * 不重新比对购物车内容与门槛。
     *
     * @param couponHistoryId 领取记录主键，不能为 {@code null}
     * @param memberId 持券会员主键，不能为 {@code null}
     * @param orderId 订单主键，不能为 {@code null}
     * @param orderSn 订单号，核销与退回都靠它找回来，不能为 {@code null}
     * @return 统一响应；{@code code} 非 0 表示券已被别的订单占用、已过有效期或状态不对
     */
    @PostMapping("/coupon/trade/lock")
    R<Void> lock(@RequestParam("couponHistoryId") Long couponHistoryId,
                 @RequestParam("memberId") Long memberId,
                 @RequestParam("orderId") Long orderId,
                 @RequestParam("orderSn") String orderSn);

    /**
     * 付款核销一张券。
     *
     * <p>幂等：支付回调重复投递时，已经核销过的订单再次调用按成功处理。
     *
     * @param orderSn 订单号，不能为 {@code null}
     * @return 统一响应；{@code code} 非 0 表示按订单号找不到已占用或已核销的券
     */
    @PostMapping("/coupon/trade/use")
    R<Void> use(@RequestParam("orderSn") String orderSn);

    /**
     * 取消或超时关单时退回一张券。
     *
     * <p>幂等且不报错：关单消息重投、以及本来就没用券的订单都会正常返回。
     *
     * @param orderSn 订单号，不能为 {@code null}
     * @return 统一响应
     */
    @PostMapping("/coupon/trade/unlock")
    R<Void> unlock(@RequestParam("orderSn") String orderSn);

}

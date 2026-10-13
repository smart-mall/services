package order.vo;

import common.to.CouponUsableVo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 订单确认页需要用的数据。五个金额都由后端算好，前端只负责显示和原样回传：
 * {@code totalAmount} = 各购物项 price × count 之和，{@code freightAmount} = 默认收货地址的运费
 * （换地址时调 {@code /order/front/jwt/fare} 重取），{@code couponAmount} = 选中券的抵扣额，
 * {@code integrationAmount} = 本单使用的积分抵掉的钱，
 * {@code payAmount} = 商品总额 + 运费 − 券抵扣 − 积分抵扣，即提交时要回传的 {@code payPrice}。
 *
 * <p>运费、券优惠与积分抵扣各有一份按商品拆分的明细（{@code fareItems}、{@code couponItems} 与
 * {@code integrationItems}），前端逐行展示商品自己的费用与优惠，不必自己再算。
 *
 * <p>{@code couponHistoryId} 与 {@code useIntegration} 是"当前选中的优惠"，由调用方在请求里指定。
 * 这两个参数变了就要重新请求本接口或 {@code /fare}：抵扣额必须由后端算，
 * 前端自己减会与提交时的校验算成两个数。
 */
@Data
public class OrderConfirmVo {

    /** 会员收货地址列表。 */
    private List<MemberAddressVo> addresses;

    /** 默认收货地址 id：优先 defaultStatus == 1，没有就取第一个；一个地址都没有时为 null。 */
    private Long defaultAddrId;

    /** 购物车里所有已勾选的购物项。 */
    private List<OrderItemVo> items;

    /**
     * 会员当前的积分余额。
     *
     * <p>取自会员服务，不取登录凭证里那一份：凭证是登录那一刻的快照，下过单之后就不再是当前余额。
     * 已按 0 归一，不会为 {@code null}；余额查不到时按 0 处理，页面不出现积分抵扣。
     */
    private Integer integration;

    /** 防重令牌。 */
    private String orderToken;

    /** skuId -> 是否有货。JSON 里 key 是字符串 */
    private Map<Long, Boolean> stocks;

    /** 商品总额（不含运费）。 */
    private BigDecimal totalAmount;

    /** 运费。 */
    private BigDecimal freightAmount;

    /** 优惠券抵扣额；没有选券时为 0。 */
    private BigDecimal couponAmount;

    /** 积分抵扣额；没有使用积分时为 0。 */
    private BigDecimal integrationAmount;

    /** 应付总额 = 商品总额 + 运费 - 优惠券抵扣额 - 积分抵扣额。 */
    private BigDecimal payAmount;

    /**
     * 本单最多可用的积分，取封顶上限与余额中的较小值。
     *
     * <p>为 0 表示本单用不了积分（封顶算下来不足 1 分，或余额为 0），前端据此隐藏积分抵扣这一栏。
     * 会员在这个上限内自选数量，超过上限提交会被拒。
     */
    private Integer integrationLimit;

    /** 当前选中的领取记录 ID；没有选券时为 {@code null}。 */
    private Long couponHistoryId;

    /**
     * 当前购物车可用的券，按抵扣额从大到小排。
     *
     * <p>没有可用券时为空列表，前端拿到的永远是数组，不用为"车是空的"和"字段没返回"写两个分支。
     * 每张券都带自己的 {@code discountAmount}，前端切换选择时可以直接算预览价，
     * 但真正提交用的 {@code payAmount} 仍以重新请求本接口拿到的为准。
     */
    private List<CouponUsableVo> availableCoupons;

    /**
     * 每个商品的运费明细，与 {@code items} 按 {@code skuId} 对应；前端逐行显示商品自己的运费。
     *
     * <p>与 {@code freightAmount} 出自同一次计费，所以两者必然自洽；不要按这份明细再求一次和，
     * 前端求和与后端汇总的舍入方式一旦不同就会差几分钱。
     */
    private List<FareItemVo> fareItems;

    /**
     * 每个商品的优惠明细，与 {@code items} 按 {@code skuId} 对应；前端逐行显示商品自己的优惠。
     *
     * <p>没有选券时为空列表，不会为 {@code null}；只含选中那张券适用范围内的商品。
     * 与 {@code couponAmount} 出自同一次分摊，两者必然自洽。
     */
    private List<CouponDiscountItemVo> couponItems;

    /**
     * 每个商品的积分抵扣明细，与 {@code items} 按 {@code skuId} 对应；前端逐行显示商品自己的积分抵扣。
     *
     * <p>没有使用积分时为空列表，不会为 {@code null}；积分没有适用范围，所以覆盖全部购物项。
     * 与 {@code integrationAmount} 出自同一次分摊，两者必然自洽。
     */
    private List<IntegrationDiscountItemVo> integrationItems;

    /**
     * 商品总件数。
     *
     * <p>是计算属性不是字段（Lombok 不会为它生成 getter）。金额一律不用这种方式算 ——
     * 金额必须有明确的、和提交校验用同一套逻辑的来源，件数没这个顾虑。</p>
     */
    public Integer getCount() {
        int count = 0;
        if (items != null) {
            for (OrderItemVo item : items) {
                if (item.getCount() != null) {
                    count += item.getCount();
                }
            }
        }
        return count;
    }

}

package order.vo;

import common.to.CouponUsableVo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 订单确认页需要用的数据。四个金额都由后端算好，前端只负责显示和原样回传：
 * {@code totalAmount} = 各购物项 price × count 之和，{@code freightAmount} = 默认收货地址的运费
 * （换地址时调 {@code /order/front/jwt/fare} 重取），{@code couponAmount} = 选中券的抵扣额，
 * {@code payAmount} = 商品总额 + 运费 - 抵扣额，即提交时要回传的 {@code payPrice}。
 *
 * <p>运费与优惠各有一份按商品拆分的明细（{@code fareItems} 与 {@code couponItems}），
 * 前端逐行展示商品自己的费用与优惠，不必自己再算。
 *
 * <p>{@code couponHistoryId} 是"当前选中的券"，由调用方在请求里指定。它变了就要重新请求本接口：
 * 抵扣额必须由后端算，前端拿券面金额自己减会与提交时的校验算成两个数。
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
     * 会员积分。
     *
     * <p>可能为 null：会员表的 integration 允许为空，前端要么不显示这一项，要么按 0 处理。</p>
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

    /** 应付总额 = 商品总额 + 运费 - 优惠券抵扣额。 */
    private BigDecimal payAmount;

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

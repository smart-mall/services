package order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 订单确认页需要用的数据。三个金额都由后端算好，前端只负责显示和原样回传：
 * {@code totalAmount} = 各购物项 price × count 之和，{@code freightAmount} = 默认收货地址的运费
 * （换地址时调 {@code /order/front/jwt/fare} 重取），{@code payAmount} = 前两者之和，即提交时要回传的 {@code payPrice}。
 */
@Data
public class OrderConfirmVo {

    /** 会员收货地址列表 */
    private List<MemberAddressVo> addresses;

    /** 默认收货地址 id：优先 defaultStatus == 1，没有就取第一个；一个地址都没有时为 null */
    private Long defaultAddrId;

    /** 购物车里所有已勾选的购物项 */
    private List<OrderItemVo> items;

    /**
     * 会员积分。
     *
     * ⚠️ 可能为 null —— 实测会员表里 integration 就是 null 的（没跑过积分逻辑），
     * 前端要么不显示这一项，要么按 0 处理。
     */
    private Integer integration;

    /** 防重令牌 */
    private String orderToken;

    /** skuId -> 是否有货。JSON 里 key 是字符串 */
    private Map<Long, Boolean> stocks;

    /** 商品总额（不含运费） */
    private BigDecimal totalAmount;

    /** 运费 */
    private BigDecimal freightAmount;

    /** 应付总额 = 商品总额 + 运费 */
    private BigDecimal payAmount;

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

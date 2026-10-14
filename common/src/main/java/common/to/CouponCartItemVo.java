package common.to;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 参与优惠券匹配的一个购物项，由 order 按购物车已勾选项组装后传给 coupon。
 *
 * <p>只带 skuId 与金额，不带 SPU 与分类：那两个由 coupon 回查商品服务补齐，
 * 免得每个调用方各自维护一份"skuId 怎么变成分类"的逻辑。
 *
 * <p>这里不写约束注解：本类型作为裸 {@code List} 的请求体元素传入，Spring 的
 * {@code @RequestBody} 校验拿不到容器元素上的约束，写在这上面的注解不会触发。
 * 校验由 coupon 在入口处显式做。
 */
@Data
public class CouponCartItemVo {

    /** SKU ID。 */
    private Long skuId;

    /** 单价。 */
    private BigDecimal price;

    /** 购买数量。 */
    private Integer count;

    /**
     * 该项命中的商品促销减免额（满减与阶梯价），由 order 算好后带过来。
     *
     * <p>券的门槛与分摊都按"促销后金额"算：先满减再用券，券看到的基数必须是会员实际要付的那部分。
     * 不带这个数时按 0 处理，也就是没有促销。
     */
    private BigDecimal promotionAmount;

    /**
     * 计算该购物项的小计。
     *
     * @return 单价 × 数量；两个字段任一为空时返回 0，调用方不必各自判空
     */
    public BigDecimal subtotal() {
        if (price == null || count == null) {
            return BigDecimal.ZERO;
        }
        return price.multiply(BigDecimal.valueOf(count));
    }

    /**
     * 计算该购物项在促销之后的小计，即券的门槛与分摊口径。
     *
     * <p>券服务算适用范围金额、order 摊券额都用它：两边必须用同一个基数，
     * 一处用小计、另一处用净额，摊出来的各项之和就对不上抵扣额。
     *
     * @return 小计减促销减免；促销减免为空时等于小计，减到负数时按 0（订单项不会出现负金额）
     */
    public BigDecimal netSubtotal() {
        BigDecimal net = subtotal().subtract(promotionAmount == null ? BigDecimal.ZERO : promotionAmount);
        return net.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : net;
    }

}

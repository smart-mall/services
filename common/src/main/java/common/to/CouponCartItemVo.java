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

}

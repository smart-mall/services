package common.to;

import lombok.Data;

/**
 * 一个 SKU 在商品层级里的归属：它属于哪个 SPU、那个 SPU 又属于哪个分类，由 product 组装后传给 coupon。
 *
 * <p>给按"指定商品"或"指定分类"匹配规则的场景用（优惠券的适用范围就是这种规则）：
 * 只给 skuId 时调用方判断不出它命中了哪条规则，而逐个 sku 回查商品信息会退化成 N 次远程调用。
 */
@Data
public class SkuScopeVo {

    /** SKU ID。 */
    private Long skuId;

    /** 该 SKU 所属的 SPU ID；SKU 没有挂 SPU 时为 {@code null}。 */
    private Long spuId;

    /** 该 SPU 所属的分类 ID；SPU 没挂分类或查不到 SPU 时为 {@code null}。 */
    private Long catalogId;

}

package common.to;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品积分设置：product 发布商品时经 Feign 传给 coupon 落库。
 */
@Data
public class SpuBoundTo {
    /** SPU ID。 */
    private Long spuId;
    /** 购物积分：购买该 SPU 可获得的购物积分。 */
    private BigDecimal buyBounds;
    /** 成长积分：购买该 SPU 可获得的成长积分。 */
    private BigDecimal growBounds;
}

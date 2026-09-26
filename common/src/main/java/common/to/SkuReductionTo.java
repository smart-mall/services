package common.to;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * SKU 优惠信息：product 发布商品时经 Feign 传给 coupon，
 * 分别写入阶梯价、满减与会员价三张表。
 */
@Data
public class SkuReductionTo {
    /** SKU ID。 */
    private Long skuId;

    /** 满几件开始打折。 */
    private Integer fullCount;
    /** 折扣（打几折）。 */
    private BigDecimal discount;
    /** 阶梯价是否能与其他优惠叠加：0 不可叠加，1 可叠加。 */
    private Integer countStatus;
    /** 满多少钱可以减价。 */
    private BigDecimal fullPrice;
    /** 减价多少钱。 */
    private BigDecimal reducePrice;
    /** 满减是否能与其他优惠叠加：0 不可叠加，1 可叠加。 */
    private Integer priceStatus;
    /** 该 SKU 不同会员等级的价格信息。 */
    private List<MemberPrice> memberPrice;

    /**
     * 该 SKU 的会员价条目。
     */
    @Data
    public static class MemberPrice {

        /** 会员等级 ID。 */
        private Long id;
        /** 会员等级名称。 */
        private String name;
        /** 会员价。 */
        private BigDecimal price;
    }

}

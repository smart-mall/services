package common.to;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SkuReductionTo {
    private Long skuId;

    /**
     * 满几件开始打折
     */
    private Integer fullCount;
    /**
     * 打几折
     */
    private BigDecimal discount;
    /**
     * 是否能与其他优惠叠加
     */
    private Integer countStatus;
    /**
     * 满多少钱可以减价
     */
    private BigDecimal fullPrice;
    /**
     * 减价多少钱
     */
    private BigDecimal reducePrice;
    /**
     * 是否能与其他优惠叠加
     */
    private Integer priceStatus;
    /**
     * 这个sku不同会员等级的价格信息
     */
    private List<MemberPrice> memberPrice;

    /**
     * 此sku的会员价
     */
    @Data
    public static class MemberPrice {

        /**
         * 会员等级表的id
         */
        private Long id;
        private String name;
        private BigDecimal price;
    }

}

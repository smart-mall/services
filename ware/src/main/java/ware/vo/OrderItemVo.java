package ware.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;


/**
 * 订单项载体：order 侧锁定库存时随 {@link WareSkuLockVo} 传来的购物项，ware 侧按其中的 SKU 与数量扣减可售库存。
 */
@Data
public class OrderItemVo {

    /** SKU ID。 */
    private Long skuId;

    /** 是否勾选；结算页只处理勾选中的项。 */
    private Boolean check;

    /** 商品标题。 */
    private String title;

    /** 商品图片地址。 */
    private String image;

    /**
     * 商品套餐属性值列表（所选规格）。
     */
    private List<String> skuAttrValues;

    /** 单价。 */
    private BigDecimal price;

    /** 购买数量，锁库存时按它扣减可售量。 */
    private Integer count;

    /** 小计，等于单价 × 数量。 */
    private BigDecimal totalPrice;

    /** 商品重量，单位千克；ware 侧锁库存只读 SKU 与数量，该字段不参与计算。 */
    private BigDecimal weight = new BigDecimal("0.085");
}

package order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;


/** 结算页与下单流程里的单个购物项。 */
@Data
public class OrderItemVo {

    /** SKU 标识。 */
    private Long skuId;

    /** 是否勾选；结算页只处理勾选中的项。 */
    private Boolean check;

    /** 商品标题。 */
    private String title;

    /** 商品图片地址。 */
    private String image;

    /** 商品套餐属性，形如「颜色:白色;内存:8GB」。 */
    private List<String> skuAttrValues;

    /** 单价。 */
    private BigDecimal price;

    /** 购买数量。 */
    private Integer count;

    /** 小计，等于单价 × 数量。 */
    private BigDecimal totalPrice;

    /** 商品重量，单位千克，用于算运费；没取到真实重量时沿用字段初始值。 */
    private BigDecimal weight = new BigDecimal("0.085");
}

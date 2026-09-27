package order.vo;

import lombok.Data;

import java.math.BigDecimal;

/** 运费明细里的单个商品，只带前端需要的两个字段。 */
@Data
public class FareItemVo {

    /** SKU 标识。 */
    private Long skuId;

    /** 该商品的运费，单位元。 */
    private BigDecimal fare;
}

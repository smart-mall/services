package order.vo;

import lombok.Data;

/** 单个 SKU 的库存查询结果。 */

@Data
public class SkuStockVo {

    /** SKU 标识。 */
    private Long skuId;

    /** 是否有货。 */
    private Boolean hasStock;

}

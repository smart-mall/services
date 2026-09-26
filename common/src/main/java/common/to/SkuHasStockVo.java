package common.to;

import lombok.Data;


/** SKU 库存查询结果。 */
@Data
public class SkuHasStockVo {

    /** SKU ID。 */
    private Long skuId;

    /** 是否有货。 */
    private Boolean hasStock;

}

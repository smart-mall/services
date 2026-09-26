package ware.vo;

import lombok.Data;

/**
 * SKU 是否有货的出参，对应 {@code POST /ware/waresku/hasStock} 与 {@code /hasstock}：
 * product 侧查商品列表与详情时按 SKU 批量取可售状态。
 */
@Data
public class SkuHasStockVo {
    /** SKU ID。 */
    private Long skuId;

    /** 是否有可售库存：该 SKU 在所有仓库的 {@code stock - stock_locked} 之和大于 0。 */
    private Boolean hasStock;
}

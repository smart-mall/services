package product.vo;

import lombok.Data;

/**
 * SKU 库存查询结果，对应 ware 服务 {@code /ware/waresku/hasstock} 响应中的一条。
 *
 * <p>由 {@code WareFeignService#getSkusHasStock} 远程取回，用于商品详情页展示是否有货，
 * 以及商品上架时给 Elasticsearch 文档填充库存字段。
 */
@Data
public class SkuHasStockVo {
    /** sku ID。 */
    private Long skuId;
    /** 是否有货；无库存的 sku 也会返回，此时该字段为 false。 */
    private Boolean hasStock;
}

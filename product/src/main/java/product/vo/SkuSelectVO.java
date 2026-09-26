package product.vo;

import lombok.Data;

/**
 * sku 下拉框选项，只含 sku ID 与名称。
 *
 * <p>是 {@code /product/skuinfo/getSkuSelect} 的出参，一次返回全表、不分页也不过滤。
 */
@Data
public class SkuSelectVO {
    /** sku ID。 */
    private Long id;
    /** sku 名称。 */
    private String name;
}

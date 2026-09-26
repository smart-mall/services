package product.vo;

import lombok.Data;

/**
 * 品牌选择项，只含品牌 ID 与品牌名。
 *
 * <p>是 {@code /product/categorybrandrelation/brands/list} 的出参，供前端按分类展示品牌列表
 * 或品牌下拉框使用。
 */
@Data
public class BrandVO {
    /** 品牌 ID，指向 {@code pms_brand.brand_id}。 */
    private Long brandId;
    /** 品牌名。 */
    private String name;
}

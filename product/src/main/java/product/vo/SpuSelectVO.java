package product.vo;

import lombok.Data;

/**
 * spu 下拉框选项，只含 spu ID 与商品名。
 *
 * <p>是 {@code /product/spuinfo/getSpuSelect} 的出参，一次返回全表、不分页也不过滤。
 */
@Data
public class SpuSelectVO {
    /** spu ID。 */
    private Long id;
    /** 商品名。 */
    private String name;
}

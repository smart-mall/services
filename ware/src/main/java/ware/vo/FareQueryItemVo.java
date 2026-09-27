package ware.vo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 运费计算入参里的单个商品：要计价的 SKU 与购买数量。 */
@Data
public class FareQueryItemVo {

    /** SKU 标识。 */
    @NotNull(message = "缺少商品标识")
    private Long skuId;

    /** 购买数量，必须大于 0；数量影响续件加价。 */
    @NotNull(message = "缺少购买数量")
    @Min(value = 1, message = "购买数量必须大于 0")
    private Integer num;
}

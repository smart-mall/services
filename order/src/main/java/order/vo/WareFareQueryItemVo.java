package order.vo;

import lombok.Data;

/** 向 ware 请求计费时的单个商品：SKU 与购买数量。 */
@Data
public class WareFareQueryItemVo {

    /** SKU 标识。 */
    private Long skuId;

    /** 购买数量。 */
    private Integer num;
}

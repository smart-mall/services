package order.vo;

import lombok.Data;

import java.math.BigDecimal;

/** ware 计费明细里的单个商品。 */
@Data
public class WareFareItemResultVo {

    /** SKU 标识。 */
    private Long skuId;

    /** 该商品的运费，单位元。 */
    private BigDecimal fare;

    /** 计费时选定的发货仓库 ID，与下单时实际锁到库存的仓库可能不同。 */
    private Long wareId;

    /** 收货地到计费仓的直线距离，单位公里。 */
    private BigDecimal distanceKm;
}

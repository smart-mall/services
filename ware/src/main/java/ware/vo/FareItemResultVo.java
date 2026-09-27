package ware.vo;

import lombok.Data;

import java.math.BigDecimal;

/** 运费计算结果里的单个商品明细。 */
@Data
public class FareItemResultVo {

    /** SKU 标识。 */
    private Long skuId;

    /** 该商品的运费，单位元，已按续件规则加价并保留 2 位小数。 */
    private BigDecimal fare;

    /**
     * 计费时选定的发货仓库 ID：候选仓里距离收货地最近的那个。
     *
     * <p>与下单时实际锁到库存的仓库可能不一致——锁库存按实时可用量逐个仓库尝试，
     * 计费仓只决定运费，不参与锁定。
     */
    private Long wareId;

    /** 收货地到计费仓的直线距离，单位公里，供核对计费依据。 */
    private BigDecimal distanceKm;
}

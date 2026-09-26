package seckill.vo;

import lombok.Data;

import java.math.BigDecimal;



/**
 * 场次关联的 SKU 秒杀配置，上架时逐条转成 {@link seckill.to.SeckillSkuRedisTo} 写入 Redis。
 */
@Data
public class SeckillSkuVo {

    /** 关联记录 ID。 */
    private Long id;

    /** 秒杀活动 ID。 */
    private Long promotionId;

    /** 秒杀场次 ID。 */
    private Long promotionSessionId;

    /** 商品 SKU ID。 */
    private Long skuId;

    /** 秒杀价格。 */
    private BigDecimal seckillPrice;

    /** 本场次参与秒杀的商品总数，上架时作为库存信号量的许可数。 */
    private Integer seckillCount;

    /** 每人限购数量。 */
    private Integer seckillLimit;

    /** 场次内的展示排序值。 */
    private Integer seckillSort;

}

package product.vo;

import lombok.Data;

import java.math.BigDecimal;


/**
 * seckill 服务返回的 SKU 秒杀信息，作为商品详情页的秒杀优惠数据。
 *
 * <p>由 {@code SeckillFeignService#getSkuSeckilInfo} 远程取回，字段与 seckill 侧的
 * {@code SeckillSkuRedisTo} 对应，取回后放进 {@link SkuItemVo#getSeckillSkuVo()}。
 */
@Data
public class SeckillSkuVo {

    /** 秒杀活动 ID。 */
    private Long promotionId;
    /** 秒杀场次 ID。 */
    private Long promotionSessionId;
    /** 参与秒杀的 sku ID。 */
    private Long skuId;
    /** 秒杀价格，单位：元。 */
    private BigDecimal seckillPrice;
    /** 本场次参与秒杀的商品总数，同时是秒杀库存信号量的许可数。 */
    private Integer seckillCount;
    /** 每人限购数量。 */
    private Integer seckillLimit;
    /** 场次内的展示排序值。 */
    private Integer seckillSort;

    /** 秒杀开始时间，毫秒时间戳。 */
    private Long startTime;

    /** 秒杀结束时间，毫秒时间戳；详情页取回后用它判断秒杀是否已结束。 */
    private Long endTime;

    /** 上架时生成的随机码，是能否抢购的凭证；只有秒杀进行中才下发，未开始或已结束时为 {@code null}。 */
    private String randomCode;

}

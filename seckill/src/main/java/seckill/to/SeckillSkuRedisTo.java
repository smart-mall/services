package seckill.to;

import seckill.vo.SkuInfoVo;
import lombok.Data;

import java.math.BigDecimal;


/**
 * 秒杀 SKU 缓存对象：上架时序列化成 JSON 写进 Redis hash，抢购时按 killId 取出校验。
 *
 * <p>下单所需的场次、价格、限购、时间区间都以这份缓存为准，前端回传的 killId 只用于定位它。
 */
@Data
public class SeckillSkuRedisTo {

    /** 秒杀活动 ID。 */
    private Long promotionId;

    /** 秒杀场次 ID。 */
    private Long promotionSessionId;

    /** 商品 SKU ID。 */
    private Long skuId;

    /** 秒杀价格。 */
    private BigDecimal seckillPrice;

    /** 本场次参与秒杀的商品总数。 */
    private Integer seckillCount;

    /** 每人限购数量。 */
    private Integer seckillLimit;

    /** 场次内的展示排序值。 */
    private Integer seckillSort;

    /** SKU 详情，上架时由 product 服务远程取回后一并缓存，避免抢购时再查一次。 */
    private SkuInfoVo skuInfo;

    /** 秒杀开始时间，毫秒时间戳。 */
    private Long startTime;

    /** 秒杀结束时间，毫秒时间戳。 */
    private Long endTime;

    /** 上架时生成的随机码，只有秒杀进行中才随详情下发，是能否抢购的凭证。 */
    private String randomCode;
}

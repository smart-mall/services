package common.to;

import lombok.Data;

import java.util.List;

/**
 * 秒杀下架请求：通知 seckill 服务把指定场次或场次下的指定商品从缓存里摘掉。
 *
 * <p>{@code skuIds} 为空表示整个场次下架，seckill 按场次前缀扫出该场次在缓存里的全部商品。
 */
@Data
public class SeckillOfflineTo {

    /** 秒杀场次 ID，不能为 {@code null}。 */
    private Long promotionSessionId;

    /** 要下架的 SKU ID 列表；为空表示下架整个场次。 */
    private List<Long> skuIds;
}

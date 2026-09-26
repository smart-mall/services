package common.to.mq;

import lombok.Data;

import java.math.BigDecimal;


/**
 * 秒杀下单消息：seckill 抢到信号量后发到 {@code order-event-exchange}（路由键 {@code order.seckill.order}），
 * order 消费后异步建单。
 */
@Data
public class SeckillOrderTo {

    /** 订单号，由 seckill 侧生成。 */
    private String orderSn;

    /** 秒杀场次 ID。 */
    private Long promotionSessionId;
    /** 秒杀商品的 SKU ID。 */
    private Long skuId;
    /** 秒杀单价。 */
    private BigDecimal seckillPrice;

    /** 购买数量。 */
    private Integer num;

    /** 下单会员 ID。 */
    private Long memberId;


}

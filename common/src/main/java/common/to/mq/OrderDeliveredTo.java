package common.to.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单发货完成的事件。由 order 在发货事务内落本地消息表，提交后发到 {@code order.exchange}，
 * ware 的 {@code ware.order.delivered.queue} 收到后把该单锁定的库存实扣掉。
 *
 * <p><b>为什么只带订单号：</b>ware 侧靠订单号回查自己的库存工作单，工作单在锁库存时就把收货人、
 * 电话与地址一起写进去了（见 {@code WareOrderTaskEntity}），本身是自包含的，发货流程不必回查订单。
 * 消息里再抄一份订单快照只会多一个会漂移的副本。</p>
 *
 * <p><b>为什么放在 common：</b>{@code Jackson2JsonMessageConverter} 会在消息头里写 {@code __TypeId__}，
 * 消费端 {@code @RabbitHandler} 按转换后的类型找方法，类不在 classpath 上就会转换失败、消息被丢弃。
 * order 发、ware 收，两边都要能加载这个类。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDeliveredTo {

    /** 已发货的订单号，ware 靠它定位库存工作单。 */
    private String orderSn;
}

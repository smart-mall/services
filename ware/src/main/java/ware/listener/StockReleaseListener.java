package ware.listener;

import com.rabbitmq.client.Channel;
import common.mq.MqConstant;
import common.to.OrderTo;
import common.to.mq.StockLockedTo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ware.service.WareSkuService;

import java.io.IOException;


/**
 * 消费 {@code stock.release.stock.queue}，释放不再需要的锁定库存。
 *
 * <p>队列上有两种消息体，按类型分派到两个 {@code @RabbitHandler}：{@link StockLockedTo} 来自本服务
 * 锁定成功后投出的消息（延迟 2 分钟到期，覆盖"下单后业务调用失败回滚"与"订单超时未支付被取消"两种情况），
 * {@link OrderTo} 由 order 侧订单关闭时投出。</p>
 *
 * <p>失败重试策略：捕获异常后 {@code basicReject(requeue=true)} 把消息放回队列重投，
 * 没有退避、没有次数上限，一条毒消息会一直被重投并持续占用消费者。</p>
 */
@Slf4j
@RabbitListener(queues = MqConstant.Queues.STOCK_RELEASE)
@Service
public class StockReleaseListener {

    @Autowired
    private WareSkuService wareSkuService;

    /**
     * 处理锁定成功但后续业务已回滚的库存，按工作单明细解锁。
     *
     * <p>解锁成功即 ack；失败用 {@code basicReject(requeue=true)} 放回队列重投。这里宁可重复投递也不能漏解：
     * 解锁只处理锁定状态仍为"已锁定"的明细，重复消费不会把库存多减一次。</p>
     *
     * @param to      库存锁定事件，含工作单 ID 与锁定的 SKU 明细
     * @param message 当前消息，从中取投递标签
     * @param channel 消费通道，用于 ack / reject
     * @throws IOException ack / reject 与 broker 通信失败时抛出
     */
    @RabbitHandler
    public void handleStockLockedRelease(StockLockedTo to, Message message, Channel channel) throws IOException {
        log.info("******收到解锁库存的信息******");
        try {


            wareSkuService.unlockStock(to);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (Exception e) {
            // requeue=true 放回队列：解锁失败多是订单服务抖动，重复投递不会重复解锁
            channel.basicReject(message.getMessageProperties().getDeliveryTag(),true);
        }
    }

    /**
     * 处理订单关闭事件，释放该订单占用的全部锁定库存。
     *
     * <p>解锁成功即 ack；失败同样 {@code basicReject(requeue=true)} 放回队列重投。</p>
     *
     * @param orderTo 订单事件，含订单号
     * @param message 当前消息，从中取投递标签
     * @param channel 消费通道，用于 ack / reject
     * @throws IOException ack / reject 与 broker 通信失败时抛出
     */
    @RabbitHandler
    public void handleOrderCloseRelease(OrderTo orderTo, Message message, Channel channel) throws IOException {

        log.info("******收到订单关闭，准备解锁库存的信息******");

        try {
            wareSkuService.unlockStock(orderTo);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (Exception e) {
            // requeue=true 放回队列：订单关闭是最终结果，漏解会让库存永远被占住
            channel.basicReject(message.getMessageProperties().getDeliveryTag(),true);
        }
    }


}

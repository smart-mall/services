package common.mq;

/**
 * MQ 拓扑常量：交换机、路由键、队列名、TTL。
 *
 * <p>字符串即物理名，与 broker 上的定义一一对应；改参数（TTL、DLX）会因参数不等价而 406。
 * 声明归属：队列由消费方声明，交换机由生产方声明。</p>
 */
public final class MqConstant {

    private MqConstant() {
    }

    /**
     * 交换机名。业务事件交换机都是 topic，各服务自己的重试死信交换机都是 direct。
     */
    public static final class Exchanges {

        private Exchanges() {
        }

        /** 订单事件交换机（topic）：下单、延迟关单、秒杀建单、订单关闭释放库存都投它。 */
        public static final String ORDER_EVENT = "order-event-exchange";
        /** 库存事件交换机（topic）：库存锁定成功进延迟队列，到期后死信成释放消息。 */
        public static final String STOCK_EVENT = "stock-event-exchange";
        /** 商品事件交换机（topic）：商品删除与下架，以及各服务重试消息的回投都走它。 */
        public static final String PRODUCT_EVENT = "product-event-exchange";
        /** coupon 重试死信交换机（direct）：接住 coupon 消费失败的消息，只转投重试队列与死信队列。 */
        public static final String COUPON_PRODUCT_DELETED_DLX = "coupon.product.deleted.dlx";
        /** third-party 重试死信交换机（direct）：接住 third-party 消费失败的消息，只转投重试队列与死信队列。 */
        public static final String THIRDPARTY_PRODUCT_DELETED_DLX = "thirdparty.product.deleted.dlx";
        /** ware 重试死信交换机（direct）：接住 ware 消费失败的消息，只转投重试队列与死信队列。 */
        public static final String WARE_PRODUCT_DELETED_DLX = "ware.product.deleted.dlx";
        /** search 重试死信交换机（direct）：接住 search 消费失败的消息，只转投重试队列与死信队列。 */
        public static final String SEARCH_PRODUCT_DOWN_DLX = "search.product.down.dlx";
    }

    /**
     * 路由键。带 {@code #} 的是 topic 绑定模式，只能用于绑定，不能当路由键投递。
     */
    public static final class RoutingKeys {

        private RoutingKeys() {
        }

        /** 下单成功，绑定 order.delay.queue，等 TTL 到期。 */
        public static final String ORDER_CREATE = "order.create.order";

        /** 关单：绑定 order.release.order.queue；同时是 order.delay.queue 的 x-dead-letter-routing-key。 */
        public static final String ORDER_RELEASE = "order.release.order";

        /** 订单关闭释放库存，由 order 投到 order-event-exchange。 */
        public static final String ORDER_RELEASE_OTHER = "order.release.other";

        /** {@code order.release.other.#} 绑定模式：ware 的 stock.release.stock.queue 靠它收 order 侧的释放消息。 */
        public static final String ORDER_RELEASE_OTHER_PATTERN = "order.release.other.#";

        /** 秒杀下单成功，绑定 order.seckill.order.queue。 */
        public static final String ORDER_SECKILL = "order.seckill.order";
        /** 库存锁定成功，绑定 stock.delay.queue，等 TTL 到期。 */
        public static final String STOCK_LOCKED = "stock.locked";

        /** 释放库存；同时是 stock.delay.queue 的 x-dead-letter-routing-key，由 STOCK_RELEASE_PATTERN 匹配。 */
        public static final String STOCK_RELEASE = "stock.release";

        /** {@code stock.release.#} 绑定模式：stock.release.stock.queue 靠它收延迟到期的释放消息。 */
        public static final String STOCK_RELEASE_PATTERN = "stock.release.#";

        /** 商品删除，product-event-exchange 上绑 coupon、third-party、ware 三个业务队列。 */
        public static final String PRODUCT_DELETED = "product.deleted";
        /** 商品下架，绑定 search.product.down.queue。 */
        public static final String PRODUCT_DOWN = "product.down";
        /** coupon 重试键：coupon 业务队列的 DLX 用它把失败消息送进 coupon 重试队列。 */
        public static final String COUPON_PRODUCT_DELETED_RETRY = "coupon.product.deleted.retry";
        /** third-party 重试键：third-party 业务队列的 DLX 用它把失败消息送进 third-party 重试队列。 */
        public static final String THIRDPARTY_PRODUCT_DELETED_RETRY = "thirdparty.product.deleted.retry";
        /** ware 重试键：ware 业务队列的 DLX 用它把失败消息送进 ware 重试队列。 */
        public static final String WARE_PRODUCT_DELETED_RETRY = "ware.product.deleted.retry";
        /** search 重试键：search 业务队列的 DLX 用它把失败消息送进 search 重试队列。 */
        public static final String SEARCH_PRODUCT_DOWN_RETRY = "search.product.down.retry";
    }

    /**
     * 队列名。死信队列的绑定路由键与队列名同值；带 dlq 后缀的队列没有消费者，等人工处理。
     */
    public static final class Queues {

        private Queues() {
        }

        /** 订单延迟队列：绑 ORDER_CREATE，TTL 到期以 ORDER_RELEASE 死信进关单队列。 */
        public static final String ORDER_DELAY = "order.delay.queue";
        /** 关单队列：收延迟到期的订单，由 OrderCloseListener 消费。 */
        public static final String ORDER_RELEASE = "order.release.order.queue";
        /** 秒杀建单队列：绑 ORDER_SECKILL，由 OrderSeckillListener 消费。 */
        public static final String ORDER_SECKILL = "order.seckill.order.queue";
        /** 库存延迟队列：绑 STOCK_LOCKED，TTL 到期以 STOCK_RELEASE 死信进释放队列。 */
        public static final String STOCK_DELAY = "stock.delay.queue";

        /** 释放库存队列：被 stock.release.# 与 order.release.other.# 绑定，收两种消息体，消费者有两个 @RabbitHandler。 */
        public static final String STOCK_RELEASE = "stock.release.stock.queue";

        /** coupon 业务队列：绑 PRODUCT_DELETED，消费失败 nack 后由 coupon.product.deleted.dlx 接走。 */
        public static final String COUPON_PRODUCT_DELETED = "coupon.product.deleted.queue";
        /** coupon 重试队列：消息躺 TTL 后死信回 product-event-exchange，等于延迟重投一次。 */
        public static final String COUPON_PRODUCT_DELETED_RETRY = "coupon.product.deleted.retry.queue";

        /** coupon 死信队列：重试到上限仍失败的消息落这里，无消费者，等人工处理。 */
        public static final String COUPON_PRODUCT_DELETED_DLQ = "coupon.product.deleted.dlq";

        /** third-party 业务队列：绑 PRODUCT_DELETED，消费失败 nack 后由 thirdparty.product.deleted.dlx 接走。 */
        public static final String THIRDPARTY_PRODUCT_DELETED = "thirdparty.product.deleted.queue";
        /** third-party 重试队列：消息躺 TTL 后死信回 product-event-exchange，等于延迟重投一次。 */
        public static final String THIRDPARTY_PRODUCT_DELETED_RETRY = "thirdparty.product.deleted.retry.queue";

        /** third-party 死信队列：重试到上限仍失败的消息落这里，无消费者，等人工处理。 */
        public static final String THIRDPARTY_PRODUCT_DELETED_DLQ = "thirdparty.product.deleted.dlq";

        /** ware 业务队列：绑 PRODUCT_DELETED，消费失败 nack 后由 ware.product.deleted.dlx 接走。 */
        public static final String WARE_PRODUCT_DELETED = "ware.product.deleted.queue";
        /** ware 重试队列：消息躺 TTL 后死信回 product-event-exchange，等于延迟重投一次。 */
        public static final String WARE_PRODUCT_DELETED_RETRY = "ware.product.deleted.retry.queue";

        /** ware 死信队列：重试到上限仍失败的消息落这里，无消费者，等人工处理。 */
        public static final String WARE_PRODUCT_DELETED_DLQ = "ware.product.deleted.dlq";

        /** search 业务队列：绑 PRODUCT_DOWN，消费失败 nack 后由 search.product.down.dlx 接走。 */
        public static final String SEARCH_PRODUCT_DOWN = "search.product.down.queue";
        /** search 重试队列：消息躺 TTL 后死信回 product-event-exchange，等于延迟重投一次。 */
        public static final String SEARCH_PRODUCT_DOWN_RETRY = "search.product.down.retry.queue";

        /** search 死信队列：重试到上限仍失败的消息落这里，无消费者，等人工处理。 */
        public static final String SEARCH_PRODUCT_DOWN_DLQ = "search.product.down.dlq";
    }

    /** 延迟 / 重试时长（毫秒），是队列参数的一部分。 */
    public static final class TtlMillis {

        private TtlMillis() {
        }

        public static final int ORDER_CLOSE = 60_000;
        public static final int STOCK_LOCK_RELEASE = 120_000;
        public static final int PRODUCT_DELETED_RETRY = 60_000;
        public static final int PRODUCT_DOWN_RETRY = 60_000;
    }
}

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

    public static final class Exchanges {

        private Exchanges() {
        }

        public static final String ORDER_EVENT = "order-event-exchange";
        public static final String STOCK_EVENT = "stock-event-exchange";
        public static final String PRODUCT_EVENT = "product-event-exchange";
        public static final String COUPON_PRODUCT_DELETED_DLX = "coupon.product.deleted.dlx";
        public static final String THIRDPARTY_PRODUCT_DELETED_DLX = "thirdparty.product.deleted.dlx";
        public static final String SEARCH_PRODUCT_DOWN_DLX = "search.product.down.dlx";
    }

    public static final class RoutingKeys {

        private RoutingKeys() {
        }

        public static final String ORDER_CREATE = "order.create.order";

        /** 同时是 order 延迟队列的 x-dead-letter-routing-key */
        public static final String ORDER_RELEASE = "order.release.order";

        public static final String ORDER_RELEASE_OTHER = "order.release.other";

        /** 绑定模式，带通配符，不能当路由键投递 */
        public static final String ORDER_RELEASE_OTHER_PATTERN = "order.release.other.#";

        public static final String ORDER_SECKILL = "order.seckill.order";
        public static final String STOCK_LOCKED = "stock.locked";

        /** 同时是 stock 延迟队列的 x-dead-letter-routing-key */
        public static final String STOCK_RELEASE = "stock.release";

        /** 绑定模式，带通配符 */
        public static final String STOCK_RELEASE_PATTERN = "stock.release.#";

        public static final String PRODUCT_DELETED = "product.deleted";
        public static final String PRODUCT_DOWN = "product.down";
        public static final String COUPON_PRODUCT_DELETED_RETRY = "coupon.product.deleted.retry";
        public static final String THIRDPARTY_PRODUCT_DELETED_RETRY = "thirdparty.product.deleted.retry";
        public static final String SEARCH_PRODUCT_DOWN_RETRY = "search.product.down.retry";
    }

    public static final class Queues {

        private Queues() {
        }

        public static final String ORDER_DELAY = "order.delay.queue";
        public static final String ORDER_RELEASE = "order.release.order.queue";
        public static final String ORDER_SECKILL = "order.seckill.order.queue";
        public static final String STOCK_DELAY = "stock.delay.queue";

        /** 收两种消息体：StockLockedTo 与 OrderTo，所以消费者有两个 @RabbitHandler */
        public static final String STOCK_RELEASE = "stock.release.stock.queue";

        public static final String COUPON_PRODUCT_DELETED = "coupon.product.deleted.queue";
        public static final String COUPON_PRODUCT_DELETED_RETRY = "coupon.product.deleted.retry.queue";

        /** 无消费者，等人工处理 */
        public static final String COUPON_PRODUCT_DELETED_DLQ = "coupon.product.deleted.dlq";

        public static final String THIRDPARTY_PRODUCT_DELETED = "thirdparty.product.deleted.queue";
        public static final String THIRDPARTY_PRODUCT_DELETED_RETRY = "thirdparty.product.deleted.retry.queue";

        /** 无消费者，等人工处理 */
        public static final String THIRDPARTY_PRODUCT_DELETED_DLQ = "thirdparty.product.deleted.dlq";

        public static final String SEARCH_PRODUCT_DOWN = "search.product.down.queue";
        public static final String SEARCH_PRODUCT_DOWN_RETRY = "search.product.down.retry.queue";

        /** 无消费者，等人工处理 */
        public static final String SEARCH_PRODUCT_DOWN_DLQ = "search.product.down.dlq";
    }

    /** 延迟 / 重试时长（毫秒），是队列参数的一部分 */
    public static final class TtlMillis {

        private TtlMillis() {
        }

        public static final int ORDER_CLOSE = 60_000;
        public static final int STOCK_LOCK_RELEASE = 120_000;
        public static final int PRODUCT_DELETED_RETRY = 60_000;
        public static final int PRODUCT_DOWN_RETRY = 60_000;
    }
}

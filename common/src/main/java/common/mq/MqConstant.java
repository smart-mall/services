package common.mq;

/**
 * 全项目 MQ 拓扑的唯一目录（catalog）。
 *
 * <p>所有交换机、路由键、队列名、TTL 都在这里定义；各服务的 {@code RabbitMQConfig}
 * 与业务代码一律引用本类，不再出现字面量。两个目的：</p>
 *
 * <ul>
 *   <li><b>一处可读</b>：系统里有哪些消息、走哪个交换机、谁发谁收，看这一个文件就够；</li>
 *   <li><b>消除重复声明</b>：同一个物理资源曾经在两个服务里各写一遍
 *       （{@code stock.release.stock.queue} 在 order 与 ware 各声明一次，
 *       只能靠注释提醒"参数必须完全一致，否则 406"）。</li>
 * </ul>
 *
 * <p><b>声明归属规则（引用本类的常量时请遵守）</b></p>
 * <ul>
 *   <li>队列由<b>它的消费方</b>声明，并由该消费方声明它到所有相关交换机的绑定；</li>
 *   <li>交换机由<b>它的生产方</b>声明。</li>
 * </ul>
 *
 * <p><b>这些字符串是物理名，不是随便起的</b>：broker 上的 exchange / queue 是持久化的。
 * 改名会让旧定义残留成孤儿队列；只改参数（TTL、DLX）则会以
 * {@code 406 PRECONDITION_FAILED} 拒绝声明、服务起不来。</p>
 *
 * @see MqBuilder 声明这些资源时的构造入口
 */
public final class MqConstant {

    private MqConstant() {
    }

    /**
     * 交换机。{@code DLX} 结尾的是死信交换机。
     *
     * <p>三个业务交换机都是 durable TopicExchange；两个 DLX 是 durable DirectExchange。</p>
     */
    public static final class Exchanges {

        private Exchanges() {
        }

        /** order 的事件交换机：下单 / 关单 / 秒杀建单。ware 也把库存释放队列绑到它上面 */
        public static final String ORDER_EVENT = "order-event-exchange";

        /** ware 的库存事件交换机 */
        public static final String STOCK_EVENT = "stock-event-exchange";

        /** product 的事件交换机：商品删除。coupon 与 third-party 各自声明队列后绑到它上面 */
        public static final String PRODUCT_EVENT = "product-event-exchange";

        /** coupon 侧商品删除的死信交换机 */
        public static final String COUPON_PRODUCT_DELETED_DLX = "coupon.product.deleted.dlx";

        /** third-party 侧商品删除的死信交换机 */
        public static final String THIRDPARTY_PRODUCT_DELETED_DLX = "thirdparty.product.deleted.dlx";
    }

    /**
     * 路由键。
     *
     * <p>以 {@code _PATTERN} 结尾的是<b>带通配符的绑定模式</b>，只能用在绑定上，
     * 不能当路由键投递（投一个带 {@code #} 的路由键没有意义）。</p>
     */
    public static final class RoutingKeys {

        private RoutingKeys() {
        }

        /** 下单成功 → 进延迟队列等关单 */
        public static final String ORDER_CREATE = "order.create.order";

        /** 延迟到期 → 进关单队列。同时也是 order 延迟队列的 {@code x-dead-letter-routing-key} */
        public static final String ORDER_RELEASE = "order.release.order";

        /** 关单 → 通知 ware 释放库存 */
        public static final String ORDER_RELEASE_OTHER = "order.release.other";

        /** ware 侧绑定 order 交换机用的模式 */
        public static final String ORDER_RELEASE_OTHER_PATTERN = "order.release.other.#";

        /** 秒杀抢购成功 → order 异步建单 */
        public static final String ORDER_SECKILL = "order.seckill.order";

        /** 锁库存成功 → 进延迟队列等回查 */
        public static final String STOCK_LOCKED = "stock.locked";

        /** 延迟到期 → 进库存释放队列。同时也是 stock 延迟队列的 {@code x-dead-letter-routing-key} */
        public static final String STOCK_RELEASE = "stock.release";

        /** 库存释放队列在 stock 交换机上的绑定模式 */
        public static final String STOCK_RELEASE_PATTERN = "stock.release.#";

        /** 商品被删除 → coupon 与 third-party 各消费一份 */
        public static final String PRODUCT_DELETED = "product.deleted";

        /** coupon 业务队列消费失败后的重试路由键（业务队列的 DLX 按它转投重试队列） */
        public static final String COUPON_PRODUCT_DELETED_RETRY = "coupon.product.deleted.retry";

        /** third-party 业务队列消费失败后的重试路由键 */
        public static final String THIRDPARTY_PRODUCT_DELETED_RETRY = "thirdparty.product.deleted.retry";
    }

    /** 队列名。 */
    public static final class Queues {

        private Queues() {
        }

        /** 延迟队列：TTL 到期后死信回 order 交换机，按 {@link RoutingKeys#ORDER_RELEASE} 进关单队列 */
        public static final String ORDER_DELAY = "order.delay.queue";

        /** 关单队列。消费方：order 的 OrderCloseListener */
        public static final String ORDER_RELEASE = "order.release.order.queue";

        /** 秒杀建单队列。消费方：order 的 OrderSeckillListener */
        public static final String ORDER_SECKILL = "order.seckill.order.queue";

        /** 延迟队列：TTL 到期后死信回 stock 交换机，按 {@link RoutingKeys#STOCK_RELEASE} 进释放队列 */
        public static final String STOCK_DELAY = "stock.delay.queue";

        /**
         * 库存释放队列。
         *
         * <p><b>两种消息体</b>都会发到它：{@code StockLockedTo}（延迟回查后解锁）与
         * {@code OrderTo}（订单关闭后解锁），所以消费方 {@code StockReleaseListener}
         * 需要两个 {@code @RabbitHandler}。它同时被 stock 与 order 两个交换机绑定。</p>
         */
        public static final String STOCK_RELEASE = "stock.release.stock.queue";

        /** coupon 侧商品删除业务队列。有 DLX，无 TTL */
        public static final String COUPON_PRODUCT_DELETED = "coupon.product.deleted.queue";

        /** coupon 侧重试队列：消息在此躺 TTL 后死信回业务交换机，等于延迟重投 */
        public static final String COUPON_PRODUCT_DELETED_RETRY = "coupon.product.deleted.retry.queue";

        /**
         * coupon 侧死信队列。没有消费者，等人工处理。
         *
         * <p>注意它的绑定路由键与队列名<b>同值</b>（见 coupon 的 {@code RabbitMQConfig}），
         * 所以此处不再单独定义路由键常量。</p>
         */
        public static final String COUPON_PRODUCT_DELETED_DLQ = "coupon.product.deleted.dlq";

        /** third-party 侧商品删除业务队列。有 DLX，无 TTL */
        public static final String THIRDPARTY_PRODUCT_DELETED = "thirdparty.product.deleted.queue";

        /** third-party 侧重试队列 */
        public static final String THIRDPARTY_PRODUCT_DELETED_RETRY = "thirdparty.product.deleted.retry.queue";

        /** third-party 侧死信队列。绑定路由键与队列名同值 */
        public static final String THIRDPARTY_PRODUCT_DELETED_DLQ = "thirdparty.product.deleted.dlq";
    }

    /**
     * 延迟 / 重试时长（毫秒）。
     *
     * <p>这是<b>队列参数</b>（{@code x-message-ttl}）的一部分，不是可以随手调的常量：
     * 改它等于改队列定义，broker 上已存在的同名队列会以 {@code 406 PRECONDITION_FAILED}
     * 拒绝声明。要改必须先删除旧队列，而队列里未消费的消息会随之丢失。</p>
     */
    public static final class TtlMillis {

        private TtlMillis() {
        }

        /** 下单后多久自动关单 */
        public static final int ORDER_CLOSE = 60_000;

        /** 锁库存成功后多久回查订单、决定是否解锁库存 */
        public static final int STOCK_LOCK_RELEASE = 120_000;

        /** 商品删除清理失败后，在重试队列里等多久再投一次 */
        public static final int PRODUCT_DELETED_RETRY = 60_000;
    }
}

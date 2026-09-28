package common.mq;

/**
 * MQ 拓扑常量：交换机、路由键、队列名、TTL。
 *
 * <p>本类不可变、无状态，仅作常量容器。字符串即 broker 上的物理名，声明时参数（TTL、DLX）
 * 必须与已存在的定义完全一致，否则 broker 以 406 PRECONDITION_FAILED 拒绝。
 * 声明归属：队列由消费方声明，交换机由生产方声明。</p>
 *
 * <p>名字一律 {@code <服务>.<事件>.<角色>}，角色取 {@code exchange} / {@code dlx} / {@code queue} /
 * {@code delay.queue} / {@code retry.queue} / {@code dlq} 之一，事件段照抄路由键。
 * 因此队列名剥掉角色后缀即为它的路由键，路由键首段加 {@code .exchange} 即它所在的交换机，
 * 绑定关系不用查表。</p>
 */
public final class MqConstant {

    private MqConstant() {
    }

    /**
     * 交换机名。业务事件交换机都是 topic（{@code <域>.exchange}）；
     * 各服务自己的重试死信交换机都是 direct（{@code <服务>.dlx}）。
     */
    public static final class Exchanges {

        private Exchanges() {
        }

        /** 订单事件交换机（topic）：下单、超时关单、秒杀建单、订单关闭释放库存都投它。 */
        public static final String ORDER = "order.exchange";
        /** 库存事件交换机（topic）：库存锁定成功进延迟队列，到期后死信成释放消息。 */
        public static final String STOCK = "stock.exchange";
        /** 商品事件交换机（topic）：商品删除与下架，以及各服务重试消息的回投都走它。 */
        public static final String PRODUCT = "product.exchange";
        /** coupon 死信交换机（direct）：接住 coupon 消费失败的消息，只转投重试队列与死信队列。 */
        public static final String COUPON_DLX = "coupon.dlx";
        /** third-party 死信交换机（direct）：接住 third-party 消费失败的消息，只转投重试队列与死信队列。 */
        public static final String THIRDPARTY_DLX = "thirdparty.dlx";
        /** ware 死信交换机（direct）：接住 ware 消费失败的消息，只转投重试队列与死信队列。 */
        public static final String WARE_DLX = "ware.dlx";
        /** search 死信交换机（direct）：接住 search 消费失败的消息，只转投重试队列与死信队列。 */
        public static final String SEARCH_DLX = "search.dlx";
    }

    /**
     * 路由键。业务事件键是 {@code <域>.<过去分词>}；基础设施键是 {@code <服务>.<事件>.retry}
     * 或 {@code <服务>.<事件>.dlq}，只准 DLX 机制使用，业务代码不得投递；带 {@code #} 的是
     * topic 绑定模式，只能用于绑定，不能当路由键投递。
     */
    public static final class RoutingKeys {

        private RoutingKeys() {
        }

        /** 下单成功，绑定 order.created.delay.queue，等 TTL 到期。 */
        public static final String ORDER_CREATED = "order.created";

        /** 超时关单：绑定 order.timed-out.queue；同时是 order.created.delay.queue 的 x-dead-letter-routing-key。 */
        public static final String ORDER_TIMED_OUT = "order.timed-out";

        /** 订单关闭释放库存，由 order 投到 order.exchange。 */
        public static final String ORDER_CLOSED = "order.closed";

        /** {@code order.closed.#} 绑定模式：ware 的 ware.stock-release.queue 靠它收 order 侧的释放消息。 */
        public static final String ORDER_CLOSED_PATTERN = "order.closed.#";

        /** 秒杀下单成功，绑定 order.seckill-created.queue。 */
        public static final String ORDER_SECKILL_CREATED = "order.seckill-created";

        /** 库存锁定成功，绑定 ware.stock.locked.delay.queue，等 TTL 到期。 */
        public static final String STOCK_LOCKED = "stock.locked";

        /** 释放库存；同时是 ware.stock.locked.delay.queue 的 x-dead-letter-routing-key。 */
        public static final String STOCK_RELEASED = "stock.released";

        /** {@code stock.released.#} 绑定模式：ware.stock-release.queue 靠它收延迟到期的释放消息。 */
        public static final String STOCK_RELEASED_PATTERN = "stock.released.#";

        /** 商品删除，product.exchange 上绑 coupon、third-party、ware 三个业务队列。 */
        public static final String PRODUCT_DELETED = "product.deleted";

        /** 商品下架，绑定 search.product.delisted.queue。 */
        public static final String PRODUCT_DELISTED = "product.delisted";

        /** coupon 重试键：coupon 业务队列的 DLX 用它把失败消息送进 coupon 重试队列。 */
        public static final String COUPON_PRODUCT_DELETED_RETRY = "coupon.product.deleted.retry";
        /** coupon 死信键：与 {@link Queues#COUPON_PRODUCT_DELETED_DLQ} 同值（direct 交换机精确匹配），重试到上限时由监听器投递。 */
        public static final String COUPON_PRODUCT_DELETED_DLQ = "coupon.product.deleted.dlq";

        /** third-party 重试键：third-party 业务队列的 DLX 用它把失败消息送进 third-party 重试队列。 */
        public static final String THIRDPARTY_PRODUCT_DELETED_RETRY = "thirdparty.product.deleted.retry";
        /** third-party 死信键：与 {@link Queues#THIRDPARTY_PRODUCT_DELETED_DLQ} 同值（direct 交换机精确匹配）。 */
        public static final String THIRDPARTY_PRODUCT_DELETED_DLQ = "thirdparty.product.deleted.dlq";

        /** ware 重试键：ware 业务队列的 DLX 用它把失败消息送进 ware 重试队列。 */
        public static final String WARE_PRODUCT_DELETED_RETRY = "ware.product.deleted.retry";
        /** ware 死信键：与 {@link Queues#WARE_PRODUCT_DELETED_DLQ} 同值（direct 交换机精确匹配）。 */
        public static final String WARE_PRODUCT_DELETED_DLQ = "ware.product.deleted.dlq";

        /** search 重试键：search 业务队列的 DLX 用它把失败消息送进 search 重试队列。 */
        public static final String SEARCH_PRODUCT_DELISTED_RETRY = "search.product.delisted.retry";
        /** search 死信键：与 {@link Queues#SEARCH_PRODUCT_DELISTED_DLQ} 同值（direct 交换机精确匹配）。 */
        public static final String SEARCH_PRODUCT_DELISTED_DLQ = "search.product.delisted.dlq";
    }

    /**
     * 队列名。首段恒为声明它的服务：业务队列 {@code <服务>.<事件>.queue}，延迟队列
     * {@code <服务>.<触发事件>.delay.queue}，重试队列 {@code <服务>.<事件>.retry.queue}，
     * 死信队列 {@code <服务>.<事件>.dlq}。事件段首段与服务名重复时只写一次，故有
     * {@code order.timed-out.queue} 而不是 {@code order.order.timed-out.queue}。
     * 带 dlq 后缀的队列没有消费者，等人工处理。
     */
    public static final class Queues {

        private Queues() {
        }

        /** 订单延迟队列：绑 ORDER_CREATED，TTL 到期以 ORDER_TIMED_OUT 死信进超时关单队列。 */
        public static final String ORDER_CREATED_DELAY = "order.created.delay.queue";
        /** 超时关单队列：收延迟到期的订单，由 OrderCloseListener 消费。 */
        public static final String ORDER_TIMED_OUT = "order.timed-out.queue";
        /** 秒杀建单队列：绑 ORDER_SECKILL_CREATED，由 OrderSeckillListener 消费。 */
        public static final String ORDER_SECKILL_CREATED = "order.seckill-created.queue";
        /** 库存延迟队列：绑 STOCK_LOCKED，TTL 到期以 STOCK_RELEASED 死信进释放队列。 */
        public static final String WARE_STOCK_LOCKED_DELAY = "ware.stock.locked.delay.queue";

        /**
         * 释放库存队列：被 stock.released.# 与 order.closed.# 绑定，收两种消息体，消费者有两个
         * {@code @RabbitHandler}。
         *
         * <p>它是全套拓扑里唯一的多键扇入点，所以按职责命名、不含单个路由键 —— 名字不符合
         * {@code <键>.queue} 形状即是「扇入队列」的信号，看到就该来读这段注释。</p>
         */
        public static final String WARE_STOCK_RELEASE = "ware.stock-release.queue";

        /** coupon 业务队列：绑 PRODUCT_DELETED，消费失败 nack 后由 coupon.dlx 接走。 */
        public static final String COUPON_PRODUCT_DELETED = "coupon.product.deleted.queue";
        /** coupon 重试队列：消息躺 TTL 后死信回 product.exchange，等于延迟重投一次。 */
        public static final String COUPON_PRODUCT_DELETED_RETRY = "coupon.product.deleted.retry.queue";
        /** coupon 死信队列：重试到上限仍失败的消息落这里，无消费者，等人工处理。 */
        public static final String COUPON_PRODUCT_DELETED_DLQ = "coupon.product.deleted.dlq";

        /** third-party 业务队列：绑 PRODUCT_DELETED，消费失败 nack 后由 thirdparty.dlx 接走。 */
        public static final String THIRDPARTY_PRODUCT_DELETED = "thirdparty.product.deleted.queue";
        /** third-party 重试队列：消息躺 TTL 后死信回 product.exchange，等于延迟重投一次。 */
        public static final String THIRDPARTY_PRODUCT_DELETED_RETRY = "thirdparty.product.deleted.retry.queue";
        /** third-party 死信队列：重试到上限仍失败的消息落这里，无消费者，等人工处理。 */
        public static final String THIRDPARTY_PRODUCT_DELETED_DLQ = "thirdparty.product.deleted.dlq";

        /** ware 业务队列：绑 PRODUCT_DELETED，消费失败 nack 后由 ware.dlx 接走。 */
        public static final String WARE_PRODUCT_DELETED = "ware.product.deleted.queue";
        /** ware 重试队列：消息躺 TTL 后死信回 product.exchange，等于延迟重投一次。 */
        public static final String WARE_PRODUCT_DELETED_RETRY = "ware.product.deleted.retry.queue";
        /** ware 死信队列：重试到上限仍失败的消息落这里，无消费者，等人工处理。 */
        public static final String WARE_PRODUCT_DELETED_DLQ = "ware.product.deleted.dlq";

        /** search 业务队列：绑 PRODUCT_DELISTED，消费失败 nack 后由 search.dlx 接走。 */
        public static final String SEARCH_PRODUCT_DELISTED = "search.product.delisted.queue";
        /** search 重试队列：消息躺 TTL 后死信回 product.exchange，等于延迟重投一次。 */
        public static final String SEARCH_PRODUCT_DELISTED_RETRY = "search.product.delisted.retry.queue";
        /** search 死信队列：重试到上限仍失败的消息落这里，无消费者，等人工处理。 */
        public static final String SEARCH_PRODUCT_DELISTED_DLQ = "search.product.delisted.dlq";
    }

    /** 延迟 / 重试时长（毫秒），是队列参数的一部分。 */
    public static final class TtlMillis {

        private TtlMillis() {
        }

        /** 下单后到自动关单的等待时长。 */
        public static final int ORDER_TIMEOUT = 60_000;
        /** 锁定库存后到自动解锁的等待时长。 */
        public static final int STOCK_LOCK_TIMEOUT = 120_000;
        public static final int PRODUCT_DELETED_RETRY = 60_000;
        public static final int PRODUCT_DELISTED_RETRY = 60_000;
    }
}

package common.to.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 商品被删除的事件。由 product 发到 {@code product-event-exchange}，
 * coupon 和 third-party 各自消费一次（两个独立队列，互不阻塞）。
 *
 * <p><b>为什么 imageUrls 要放在消息里，而不是让消费方回查：</b>
 * 这条消息是在商品行<b>已经删掉之后</b>才发出去的，消费方再去查 product 是查不到的。
 * 所以删除时引用到的所有文件地址必须在消息里带走。</p>
 *
 * <p><b>为什么放在 common 而不是 product：</b>coupon 和 third-party 都要靠这个类反序列化。
 * {@code Jackson2JsonMessageConverter} 会在消息头里写 {@code __TypeId__}，
 * 消费端 {@code @RabbitHandler} 按转换后的类型找方法，类不在 classpath 上就会转换失败、
 * 消息被丢弃。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDeletedTo {

    /** 被删除的 spu id */
    private List<Long> spuIds;

    /** 这些 spu 下的所有 sku id */
    private List<Long> skuIds;

    /** 商品引用到的全部文件地址（spu 图集 / sku 图集 / sku 默认图 / 描述图），已去重 */
    private List<String> imageUrls;
}

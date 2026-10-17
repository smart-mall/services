package common.to.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分类被删除的事件。由 product 发到 {@code product.exchange}，只有 coupon 一个队列消费：
 * 清掉这些分类留下的失效券范围行。
 *
 * <p><b>为什么带整棵子树的 ID，而不是调用方传进来的那几个根节点：</b>删除是连子分类一起删的，
 * 而券的分类范围可以配在任意一层，只有把本次真正删掉的全部分类带给 coupon，失效范围行才清得干净。
 *
 * <p><b>为什么放在 common 而不是 product：</b>coupon 要靠这个类反序列化。
 * {@code Jackson2JsonMessageConverter} 会在消息头里写 {@code __TypeId__}，
 * 消费端 {@code @RabbitHandler} 按转换后的类型找方法，类不在 classpath 上就会转换失败、
 * 消息被丢弃。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDeletedTo {

    /** 本次被删除的全部分类 ID，含子树里的子分类。 */
    private List<Long> catIds;
}

package common.to.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 商品下架事件。product 发到 {@code product-event-exchange}，search 消费后清掉这些 spu 在 ES 里的文档。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDownTo {

    /** 被下架的 spu id */
    private List<Long> spuIds;
}

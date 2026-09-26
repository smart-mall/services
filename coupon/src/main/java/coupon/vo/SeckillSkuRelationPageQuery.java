package coupon.vo;

import common.query.PageQuery;
import lombok.Getter;
import lombok.Setter;

/**
 * 秒杀活动商品关联列表的查询条件。
 *
 * <p>在分页参数之外只承载一个筛选条件：场次 ID，用来把列表限定到某一场秒杀。
 */
@Getter
@Setter
public class SeckillSkuRelationPageQuery extends PageQuery {

    /** 秒杀活动场次 ID，非空时按场次精确筛选，为空时不加条件 */
    private String promotionSessionId;
}

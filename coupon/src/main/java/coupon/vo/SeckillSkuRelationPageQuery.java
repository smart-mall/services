package coupon.vo;

import common.query.PageQuery;
import lombok.Getter;
import lombok.Setter;

/** 秒杀商品关联列表的查询条件 */
@Getter
@Setter
public class SeckillSkuRelationPageQuery extends PageQuery {

    private String promotionSessionId;
}

package order.vo;

import common.query.PageQuery;
import lombok.Getter;
import lombok.Setter;

/** 我的订单列表的查询条件 */
@Getter
@Setter
public class OrderPageQuery extends PageQuery {

    private String status;
}

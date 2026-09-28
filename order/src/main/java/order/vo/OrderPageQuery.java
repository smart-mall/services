package order.vo;

import common.query.PageQuery;
import lombok.Getter;
import lombok.Setter;

/** 我的订单列表的查询条件。 */
@Getter
@Setter
public class OrderPageQuery extends PageQuery {

    /** 订单状态筛选，取值见 {@link order.enums.OrderStatusEnum}；不传则返回全部状态。 */
    private String status;
}

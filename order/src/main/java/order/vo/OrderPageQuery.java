package order.vo;

import common.query.PageQuery;
import lombok.Getter;
import lombok.Setter;

/**
 * 订单列表的查询条件，管理端与会员端共用。
 *
 * <p>会员端只用 {@code status}，管理端另用 {@code key}、下单时间区间与删除状态；
 * 除分页外的字段都为空时不过滤。</p>
 */
@Getter
@Setter
public class OrderPageQuery extends PageQuery {

    /** 订单状态筛选，取值见 {@link order.enums.OrderStatusEnum}；不传则返回全部状态。 */
    private String status;

    /** 关键字，同时匹配订单号、收货人与下单会员名；不传则不按关键字过滤。 */
    private String key;

    /** 下单时间下界，格式 {@code yyyy-MM-dd HH:mm:ss}；不传则不限制。 */
    private String startTime;

    /** 下单时间上界，格式 {@code yyyy-MM-dd HH:mm:ss}；不传则不限制。 */
    private String endTime;

    /**
     * 删除状态筛选，取值见 {@code oms_order.delete_status}：0 未删除、1 已删除（会员删过）；
     * 不传则返回全部。
     */
    private String deleteStatus;
}

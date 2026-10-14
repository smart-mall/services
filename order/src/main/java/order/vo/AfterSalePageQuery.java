package order.vo;

import common.query.PageQuery;
import lombok.Getter;
import lombok.Setter;

/**
 * 后台售后台账的查询条件。
 *
 * <p>这个页面默认看到的是"还没处理完的单"，但不过滤状态也可以（{@code status} 不传即返回全部）——
 * 已拒绝与已完成的行必须查得到，否则会员回来问"我那单退得怎么样"时没有入口。
 *
 * <p>除分页外的字段都为空时不过滤。
 */
@Getter
@Setter
public class AfterSalePageQuery extends PageQuery {

    /** 售后类型筛选，取值见 {@link order.enums.AfterSaleTypeEnum}；不传则两种都返回。 */
    private String type;

    /** 申请状态筛选，取值见 {@link order.enums.AfterSaleStatusEnum}；不传则返回全部状态。 */
    private String status;

    /** 关键字，同时匹配订单号与申请会员名；不传则不按关键字过滤。 */
    private String key;

    /** 申请时间下界，格式 {@code yyyy-MM-dd HH:mm:ss}；不传则不限制。 */
    private String startTime;

    /** 申请时间上界，格式 {@code yyyy-MM-dd HH:mm:ss}；不传则不限制。 */
    private String endTime;
}

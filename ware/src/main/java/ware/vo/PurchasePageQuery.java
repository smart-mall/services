package ware.vo;

import common.query.PageQuery;
import lombok.Getter;
import lombok.Setter;

/**
 * 采购单列表的查询条件，对应 {@code GET /ware/purchase/list}。
 */
@Getter
@Setter
public class PurchasePageQuery extends PageQuery {

    /** 关键词：按采购单 ID 精确匹配，或按采购员姓名模糊匹配。 */
    private String key;

    /** 采购单状态码，取值见 {@link ware.constants.PurchaseStatusEnum}；不传不过滤。 */
    private String status;
}

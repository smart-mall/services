package ware.vo;

import common.query.PageQuery;
import lombok.Getter;
import lombok.Setter;

/**
 * 采购需求单列表的查询条件，对应 {@code GET /ware/purchasedetail/list}。
 */
@Getter
@Setter
public class PurchaseDetailPageQuery extends PageQuery {

    /** 关键词：按 SKU 名称或 SKU ID 包含匹配，在当页结果内过滤。 */
    private String key;

    /** 采购需求单状态码，取值见 {@link ware.constants.PurchaseDetailEnum}；不传不过滤。 */
    private String status;

    /** 仓库 ID，精确匹配；不传不过滤。 */
    private String wareId;

    /** 采购单 ID，用于取某张单下的全部明细 —— 完成采购要逐条填结果。 */
    private String purchaseId;
}

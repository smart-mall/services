package ware.vo;

import common.query.PageQuery;
import lombok.Getter;
import lombok.Setter;

/** 采购单列表的查询条件 */
@Getter
@Setter
public class PurchasePageQuery extends PageQuery {

    private String key;

    private String status;
}

package ware.vo;

import common.query.PageQuery;
import lombok.Getter;
import lombok.Setter;

/** 库存列表的查询条件 */
@Getter
@Setter
public class WareSkuPageQuery extends PageQuery {

    private String skuId;

    private String wareId;
}

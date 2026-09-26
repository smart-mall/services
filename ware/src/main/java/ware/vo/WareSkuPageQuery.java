package ware.vo;

import common.query.PageQuery;
import lombok.Getter;
import lombok.Setter;

/**
 * 库存列表的查询条件，对应 {@code GET /ware/waresku/list}。
 */
@Getter
@Setter
public class WareSkuPageQuery extends PageQuery {

    /** SKU ID，精确匹配；不传不过滤。 */
    private String skuId;

    /** 仓库 ID，精确匹配；不传不过滤。 */
    private String wareId;
}

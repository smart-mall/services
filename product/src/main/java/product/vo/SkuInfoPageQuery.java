package product.vo;

import common.query.PageQuery;
import lombok.Getter;
import lombok.Setter;

/** sku 列表的查询条件。min / max 是价格区间，不传就不过滤 */
@Getter
@Setter
public class SkuInfoPageQuery extends PageQuery {

    private String key;

    private String catalogId;

    private String brandId;

    private Integer min;

    private Integer max;
}

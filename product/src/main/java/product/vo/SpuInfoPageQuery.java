package product.vo;

import common.query.PageQuery;
import lombok.Getter;
import lombok.Setter;

/** 商品列表（spu）的查询条件 */
@Getter
@Setter
public class SpuInfoPageQuery extends PageQuery {

    private String key;

    private String status;

    private String brandId;

    private String catalogId;
}

package product.vo;

import common.query.PageQuery;
import lombok.Getter;
import lombok.Setter;

/**
 * 商品列表（spu）的查询条件，对应 {@code /product/spuinfo/list} 的查询参数。
 *
 * <p>各筛选字段为空时不参与过滤；分页字段 {@code page} 与 {@code limit} 继承自 {@link PageQuery}。
 */
@Getter
@Setter
public class SpuInfoPageQuery extends PageQuery {

    /** 关键字，匹配 spu ID 或商品名；为空或空串时不参与过滤。 */
    private String key;

    /** 上架状态：0 新建，1 已上架，2 已下架；为空或空串时不参与过滤。 */
    private String status;

    /** 品牌 ID 的字符串形式；为空、空串或 {@code "0"} 时不参与过滤。 */
    private String brandId;

    /** 所属三级分类 ID 的字符串形式；为空、空串或 {@code "0"} 时不参与过滤。 */
    private String catalogId;
}

package product.vo;

import common.query.PageQuery;
import lombok.Getter;
import lombok.Setter;

/**
 * sku 列表的查询条件，对应 {@code /product/skuinfo/list} 的查询参数。
 *
 * <p>各筛选字段为空时不参与过滤；分页字段 {@code page} 与 {@code limit} 继承自 {@link PageQuery}。
 */
@Getter
@Setter
public class SkuInfoPageQuery extends PageQuery {

    /** 关键字，匹配 sku ID 或 sku 名；为空或空串时不参与过滤。 */
    private String key;

    /** 所属三级分类 ID 的字符串形式；为空、空串或 {@code "0"} 时不参与过滤。 */
    private String catalogId;

    /** 品牌 ID 的字符串形式；为空、空串或 {@code "0"} 时不参与过滤。 */
    private String brandId;

    /** 价格区间下限，单位：元；只有 {@code min} 与 {@code max} 同时给出且 {@code 0 <= min < max} 时才过滤。 */
    private Integer min;

    /** 价格区间上限，单位：元；过滤条件与 {@link #min} 相同。 */
    private Integer max;
}

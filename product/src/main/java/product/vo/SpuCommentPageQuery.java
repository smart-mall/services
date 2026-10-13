package product.vo;

import common.query.PageQuery;
import lombok.Getter;
import lombok.Setter;

/**
 * 后台评价审核列表的查询条件，对应 {@code /product/spucomment/list} 的查询参数。
 *
 * <p>各筛选字段为空时不参与过滤；分页字段 {@code page} 与 {@code limit} 继承自 {@link PageQuery}。
 *
 * <p>没有"评论类型"这一项：列表固定按直接评价分页，回复跟着各自的父评价一起下发。
 */
@Getter
@Setter
public class SpuCommentPageQuery extends PageQuery {

    /** 被评价的 SKU ID；为空时不参与过滤。 */
    private Long skuId;

    /** 被评价的 SPU ID；为空时不参与过滤。 */
    private Long spuId;

    /** 显示状态，只应传 0（已下架）或 1（显示）；为空时不参与过滤。 */
    private Integer showStatus;

    /** 关键字，模糊匹配评价内容、商品名或会员昵称；为空或空串时不参与过滤。 */
    private String key;
}

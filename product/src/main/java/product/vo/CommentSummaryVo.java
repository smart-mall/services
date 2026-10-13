package product.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 某件商品的评价统计，供详情页的评论区显示总数、平均分与星级分布。
 *
 * <p>统计口径与列表完全一致（只算可见的直接评价），否则会出现"标题写 10 条、列表点开只有 6 条"。
 */
@Data
public class CommentSummaryVo {

    /** 评价总数。 */
    private long total;
    /** 平均星级，保留一位小数；没有评价时为 0。 */
    private BigDecimal averageStar;
    /**
     * 星级分布：key 是星级 1~5，value 是该星级的条数。
     *
     * <p>没有评价的星级不会出现在表里，前端按"缺省即 0"处理。
     */
    private Map<Integer, Integer> starCounts;

}

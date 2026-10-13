package common.to;

import lombok.Data;

import java.util.Date;

/**
 * 评价摘要：product 交给 order 的最小信息，供会员订单列表判断"这一项评过没有"并渲染查看用。
 *
 * <p>不带会员也不带商品字段：调用方是订单侧，它手上已经有订单项与会员身份，摘要只补"评价本身"
 * 这件事 —— 何时评的、几星、说了什么。
 *
 * <p>放在 {@code common/to} 是因为它是 order 与 product 之间的契约：两边各存一份副本时，
 * 一边加字段、另一边没加不会编译报错，Jackson 只把缺的字段读成 {@code null}，联调也看不出来。
 */
@Data
public class CommentBriefVo {

    /** 评价主键，删除时用它。 */
    private Long commentId;
    /** 来源订单项 ID，调用方按它把摘要挂回订单项。 */
    private Long orderItemId;
    /** 星级评分，1~5。 */
    private Integer star;
    /** 评价内容。 */
    private String content;
    /** 评价时间。 */
    private Date createTime;

}

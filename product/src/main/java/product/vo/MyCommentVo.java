package product.vo;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 评论中心里的一条评价，{@code /comment/my} 的返回投影。
 *
 * <p>不是 {@code SpuCommentEntity}：那一行里还有 {@code memberId}、{@code memberIp}、
 * {@code showStatus} 这些接口不该下发的列。除评价自身外还带上获赞数、回复数与回复内容，
 * 会员在这里就能看到别人对自己评价的反馈，不必回到商品页。
 *
 * <p>{@code star}、{@code content} 与 {@code createTime} 展示时直接用；{@code skuId}
 * 是跳回商品详情的入口。
 */
@Data
public class MyCommentVo {

    /** 评价主键。 */
    private Long id;
    /** 来源订单项 ID，指向 {@code oms_order_item.id}。 */
    private Long orderItemId;
    /** 被评价的 SKU ID，用于跳回商品详情页。 */
    private Long skuId;
    /** 商品名快照，下单那一刻的名字。 */
    private String spuName;
    /** 购买时的销售属性组合，用 ";" 连接。 */
    private String spuAttributes;
    /** 星级评分，1~5。 */
    private Integer star;
    /** 评价内容。 */
    private String content;
    /** 评价时间。 */
    private Date createTime;
    /** 获赞数，没有点赞时为 0。 */
    private Integer likesCount;
    /** 回复数，没有回复时为 0。 */
    private Integer replyCount;
    /** 别人在这条评价下的回复，按时间正序；没有回复时为空列表，不是 {@code null}。 */
    private List<CommentReplyVo> replies;

}

package product.vo;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 商品详情页评论区的单条评价，公开接口的返回投影。
 *
 * <p>刻意不是 {@code SpuCommentEntity}：那一行里还有 {@code memberId}（会员身份）与
 * {@code memberIp}（IP），外加 {@code showStatus}、{@code commentType}、{@code orderItemId}
 * 这些前台用不到也不该看到的列。公开出去的东西要按字段挑，不能把整行甩出去。
 *
 * <p>点赞与回复都挂在这一层：{@code likesCount} 是计数列，{@code replies} 是回复内容。
 * 回复不分页，一条评价的回复随列表一起带回，所以 {@code replies} 就是全部。
 */
@Data
public class SpuCommentVo {

    /** 评价主键。 */
    private Long id;
    /** 星级评分，1~5。 */
    private Integer star;
    /** 评价内容。 */
    private String content;
    /** 会员昵称，已打码：保留前三个字符，不足三个只留第一个。 */
    private String memberNickName;
    /** 购买时的销售属性组合，用 ";" 连接；同款不同规格的评价靠它区分。 */
    private String spuAttributes;
    /** 评价时间。 */
    private Date createTime;
    /** 获赞数，没有点赞时为 0。 */
    private Integer likesCount;
    /** 回复数，没有回复时为 0。回复内容下发的是可见的那些，计数列与它可能不等。 */
    private Integer replyCount;
    /** 这条评价下的回复，按时间正序；没有回复时为空列表，不是 {@code null}。 */
    private List<CommentReplyVo> replies;

}

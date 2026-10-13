package product.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

/**
 * 后台评价审核列表的一行：一条直接评价，它的回复挂在 {@link #replies} 下。
 *
 * <p>与公开投影 {@link SpuCommentVo} 的区别：昵称原文、带会员 ID，且 {@link #showStatus} 如实下发 ——
 * 后台要能看到已下架的行才谈得上把它们恢复，所以审核查询不过滤显示状态。
 *
 * <p>回复只有一层，本类不会在 {@link #replies} 里再嵌下一级。
 */
@Getter
@Setter
public class CommentAuditVo {

    /** 评价主键。 */
    private Long id;

    /** 被评价的 SPU ID，指向 {@code pms_spu_info.id}。 */
    private Long spuId;

    /** 被评价的 SKU ID，指向 {@code pms_sku_info.sku_id}。 */
    private Long skuId;

    /** 商品名快照。 */
    private String spuName;

    /** 评价人会员 ID，指向 {@code ums_member.id}。 */
    private Long memberId;

    /** 评价人昵称原文，不打码；会员没留昵称时为空。 */
    private String memberNickName;

    /** 星级评分；后台脚手架造出来的行可能为空。 */
    private Integer star;

    /** 评价内容。 */
    private String content;

    /** 评价时间。 */
    private Date createTime;

    /** 显示状态[0-不显示，1-显示]。 */
    private Integer showStatus;

    /** 点赞数，计数列为空时归一成 0，不会为 {@code null}。 */
    private Integer likesCount;

    /** 回复数，计数列为空时归一成 0，不会为 {@code null}。 */
    private Integer replyCount;

    /** 这条评价下的回复，含已下架的那些；没有回复时为空列表。 */
    private List<CommentAuditReplyVo> replies;
}

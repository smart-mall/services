package product.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * 后台审核视图里挂在主评价下的一条回复。
 *
 * <p>与公开投影 {@link CommentReplyVo} 的区别：昵称保持原文、带会员 ID —— 后台要认得出人才能判断怎么处理，
 * 公开区不该看到的信息在这里可以出现，因为这个投影只下发给管理端接口。
 */
@Getter
@Setter
public class CommentAuditReplyVo {

    /** 回复主键，指向 {@code pms_spu_comment.id} 里 {@code commentType = 1} 的那些行。 */
    private Long id;

    /** 发表回复的会员 ID，指向 {@code ums_member.id}。 */
    private Long memberId;

    /** 会员昵称原文，不打码；会员没留昵称时为空。 */
    private String memberNickName;

    /** 回复内容。 */
    private String content;

    /** 回复时间。 */
    private Date createTime;

    /** 显示状态[0-不显示，1-显示]。 */
    private Integer showStatus;

    /** 点赞数，计数列为空时归一成 0，不会为 {@code null}。 */
    private Integer likesCount;
}

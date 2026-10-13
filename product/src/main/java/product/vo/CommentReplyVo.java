package product.vo;

import lombok.Data;

import java.util.Date;

/**
 * 一条评价下的回复，公开接口的返回投影。
 *
 * <p>刻意不含 {@code memberId}：回复与评价一样是给所有人看的，谁发的靠打码昵称体现，
 * 会员身份不下发。回复不再往下嵌套，所以这里没有子回复字段。
 */
@Data
public class CommentReplyVo {

    /** 回复主键，指向 {@code pms_spu_comment.id} 里 {@code commentType = 1} 的那些行。 */
    private Long id;
    /** 回复人昵称，已打码：保留前三个字符，不足三个只留第一个。 */
    private String memberNickName;
    /** 回复内容。 */
    private String content;
    /** 回复时间。 */
    private Date createTime;
    /** 获赞数，没人点赞时为 0。 */
    private Integer likesCount;

}

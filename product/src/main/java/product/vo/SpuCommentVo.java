package product.vo;

import lombok.Data;

import java.util.Date;

/**
 * 商品详情页评论区的单条评价，公开接口的返回投影。
 *
 * <p>刻意不是 {@code SpuCommentEntity}：那一行里还有 {@code memberId}（会员身份）与
 * {@code memberIp}（IP），外加 {@code showStatus}、{@code commentType}、{@code orderItemId}
 * 这些前台用不到也不该看到的列。公开出去的东西要按字段挑，不能把整行甩出去。
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

}

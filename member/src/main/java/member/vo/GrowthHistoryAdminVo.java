package member.vo;

import lombok.Data;

import java.util.Date;

/**
 * 管理端成长值流水台账的行。
 *
 * <p>与会员端的 {@link GrowthHistoryVo} 分开：台账要看"谁的成长值怎么变的"，
 * 因此额外带 {@code memberId} 与 {@code memberNickname}，而会员看自己的流水时这两项没有信息量。
 *
 * <p>{@code memberNickname} 由 {@code MemberService#getMemberNames} 批量补上，
 * 会员被删除或没有昵称时为 {@code null}。
 */
@Data
public class GrowthHistoryAdminVo {

    /** 主键，供前端列表做稳定 key。 */
    private Long id;

    /** 所属会员 ID。 */
    private Long memberId;

    /** 会员昵称；会员不存在或没有昵称时为 {@code null}。 */
    private String memberNickname;

    /** 变动时间。 */
    private Date createTime;

    /** 变动值，正数表示增加；成长值没有扣减路径，本字段恒为正数。 */
    private Integer changeCount;

    /** 变动类型取值，取值见 {@code GrowthChangeTypeEnum}；没有订单来源的变动为 {@code null}。 */
    private Integer changeType;

    /** 变动类型文案；没有订单来源的变动为 {@code null}，前端按"其他变动"显示。 */
    private String changeTypeText;

    /** 关联订单号；没有订单来源的变动为 {@code null}。 */
    private String orderSn;

    /** 备注。 */
    private String note;

}

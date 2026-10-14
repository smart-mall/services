package member.vo;

import lombok.Data;

import java.util.Date;

/**
 * 管理端积分流水台账的行。
 *
 * <p>与会员端的 {@link IntegrationHistoryVo} 分开：台账要看"谁的积分怎么变的"，
 * 因此额外带 {@code memberId} 与 {@code memberNickname}，而会员看自己的流水时这两项没有信息量。
 *
 * <p>不返回 {@code sourceType}：现有写入方（{@code IntegrationAccountService}）只写
 * {@code SOURCE_TYPE_SHOPPING} 一个取值，台账上它会是一列常量，变动分类由 {@code changeType} 承载；
 * 与 {@code GrowthHistoryAdminVo} 的取字段口径保持一致。将来真出现管理员手工调整，再把来源字段加回来。
 *
 * <p>{@code memberNickname} 由 {@code MemberService#getMemberNames} 批量补上，
 * 会员被删除或没有昵称时为 {@code null}。
 */
@Data
public class IntegrationHistoryAdminVo {

    /** 主键，供前端列表做稳定 key。 */
    private Long id;

    /** 所属会员 ID。 */
    private Long memberId;

    /** 会员昵称；会员不存在或没有昵称时为 {@code null}。 */
    private String memberNickname;

    /** 流水生成时间。 */
    private Date createTime;

    /** 变动值，正数表示增加、负数表示扣减。 */
    private Integer changeCount;

    /** 变动类型取值，取值见 {@code IntegrationChangeTypeEnum}；手工调整这类变动为 {@code null}。 */
    private Integer changeType;

    /** 变动类型文案；取值不属于已知类型时为 {@code null}，前端按"其他变动"显示。 */
    private String changeTypeText;

    /** 关联订单号；没有订单来源的变动为 {@code null}。 */
    private String orderSn;

    /** 备注。 */
    private String note;

}

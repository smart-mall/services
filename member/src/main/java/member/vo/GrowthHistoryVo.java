package member.vo;

import lombok.Data;

import java.util.Date;

/**
 * 会员端「我的成长值」页的流水行。
 *
 * <p>由 {@code GrowthChangeHistoryService#queryMine} 组装，只暴露会员看得懂的四项：
 * 时间、变动值、变动类型文案与来源订单号。不返回 {@code memberId} 与 {@code sourceType} ——
 * 前者对本人的列表没有信息量，后者是后台口径的取值。
 */
@Data
public class GrowthHistoryVo {

    /** 主键，供前端列表做稳定 key。 */
    private Long id;

    /** 变动时间。 */
    private Date createTime;

    /** 变动值，正数表示增加；成长值没有扣减路径，本字段恒为正数。 */
    private Integer changeCount;

    /** 变动类型取值，取值见 {@code GrowthChangeTypeEnum}；没有订单来源的变动为 {@code null}。 */
    private Integer changeType;

    /** 变动类型文案，如"下单赠送"；没有订单来源的变动为 {@code null}，前端按"其他变动"显示。 */
    private String changeTypeText;

    /** 关联订单号；没有订单来源的变动为 {@code null}。 */
    private String orderSn;

    /** 备注。 */
    private String note;

}

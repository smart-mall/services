package common.to;

import lombok.Data;

/**
 * 会员成长状态快照：当前成长值、所属等级与该等级的门槛。
 *
 * <p>放在 {@code common/to} 是它是 member 与 coupon 之间的契约。等级与成长值都不进登录凭证，
 * 需要判定的服务按 {@code memberId} 向 member 取实时值 —— 凭证是登录那一刻的快照，
 * 升级之后就不再是当前等级。
 *
 * <p>判定等级高低要比 {@code growthPoint} 而不是 {@code growth}：等级是记在 {@code ums_member.level_id}
 * 上的，且只升不降，门槛被调高时会员仍留在原等级，此时按成长值推出来的档会低于他实际所在的档。
 * 两个门槛都是某个等级的 {@code growth_point}，互相比较才是"哪个等级更高"。
 */
@Data
public class MemberGrowthVo {

    /** 会员当前等级主键；会员没有等级（等级表为空或未配默认等级）时为 {@code null}。 */
    private Long levelId;

    /** 会员当前等级名称；会员没有等级时为 {@code null}。 */
    private String levelName;

    /** 成长值累计值；从未有过成长值来源时为 0，不返回 {@code null}。 */
    private Integer growth;

    /** 当前等级的门槛；会员没有等级时为 {@code null}。 */
    private Integer growthPoint;

    /** 下一等级的名称；已是最高等级或等级表为空时为 {@code null}。 */
    private String nextLevelName;

    /** 下一等级的成长值门槛；已是最高等级或等级表为空时为 {@code null}。 */
    private Integer nextGrowthPoint;

}

package common.to;

import lombok.Data;

/**
 * 会员等级定义：一个等级的名称与成长值门槛。
 *
 * <p>放在 {@code common/to} 是它是 member 与 coupon 之间的契约：等级表在 member 库，
 * 券的领取门槛要按成长值比，而 coupon 不去读别人的表。两个服务各存一份副本时，
 * 一边加字段、另一边没加不会编译报错，Jackson 只会把缺的字段读成 {@code null}。
 */
@Data
public class MemberLevelVo {

    /** 等级主键，指向 {@code ums_member_level.id}。 */
    private Long id;

    /** 等级名称，如"铜牌会员"；领取被拒时直接显示给会员。 */
    private String name;

    /** 达到该等级所需的成长值门槛。 */
    private Integer growthPoint;

}

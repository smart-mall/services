package member.service;

import common.to.MemberGrowthVo;
import common.to.MemberLevelVo;

import java.util.List;

/**
 * 会员成长值账户的变动入口：支付赠送、评价赠送、等级重算与成长状态查询。
 *
 * <p>实现方必须保证：同一 {@code orderSn} 的同一类变动重复调用只落账一次（幂等），
 * 调用方因此可以直接重试而不必自己去重；成长值累加之后等级在同一事务内重算，
 * 不出现"成长值加上了、等级还停在旧档"的中间态。
 */
public interface GrowthAccountService {

    /**
     * 支付成功时赠送成长值，把累计成长值加上指定数量。
     *
     * <p>幂等：支付回调会重投，重复调用只赠一次，第二次起按成功返回。
     *
     * @param memberId 会员 id，不能为 {@code null}
     * @param orderSn 订单号，必填；它是幂等判据，也是会员端流水的来源凭据
     * @param points 赠送数量，必须为正数
     * @throws common.exception.BaseException 会员不存在（15002）时抛出
     */
    void grant(Long memberId, String orderSn, int points);

    /**
     * 评价成功后赠送成长值，数量取会员当前等级配置的每次评价成长值。
     *
     * <p>数量不由调用方传：送多少是会员侧的等级规则，调用方不必知道 {@code ums_member_level}
     * 的结构。会员没有等级、或该等级把每次评价成长值配成 0 时按无操作返回，不抛出 ——
     * 这两种都是合法配置，抛错会把会员的评价提交整个挡住。
     *
     * <p>幂等：同一订单重复调用只赠一次。
     *
     * @param memberId 会员 id，不能为 {@code null}
     * @param orderSn 订单号，必填；幂等判据
     * @throws common.exception.BaseException 会员不存在（15002）时抛出
     */
    void grantByComment(Long memberId, String orderSn);

    /**
     * 查询会员的成长状态快照：当前成长值、所属等级与该等级的门槛。
     *
     * <p>无副作用。给券服务判断领取门槛用，取的是实时值而不是登录凭证里的快照。
     *
     * @param memberId 会员 id，不能为 {@code null}
     * @return 成长状态快照；会员没有等级时 {@code levelId}、{@code levelName} 与
     *         {@code growthPoint} 为 {@code null}，{@code growth} 归一到 0
     * @throws common.exception.BaseException 会员不存在（15002）时抛出
     */
    MemberGrowthVo snapshot(Long memberId);

    /**
     * 查询全部会员等级定义，按成长值门槛升序。
     *
     * <p>无副作用。等级表只有几行，调用方按 id 取门槛用。
     *
     * @return 等级定义列表；等级表为空时返回空列表，不返回 {@code null}
     */
    List<MemberLevelVo> listLevels();
}

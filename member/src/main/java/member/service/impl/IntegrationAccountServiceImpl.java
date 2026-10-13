package member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import common.exception.BaseCodeEnum;
import common.exception.BaseException;
import lombok.extern.slf4j.Slf4j;
import member.dao.MemberDao;
import member.entity.IntegrationChangeHistoryEntity;
import member.entity.MemberEntity;
import member.enums.IntegrationChangeTypeEnum;
import member.service.IntegrationAccountService;
import member.service.IntegrationChangeHistoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * 会员积分账户的变动实现：查余额、下单抵扣、关单退回、支付赠送。
 *
 * <p>无状态、线程安全。三条贯穿全类的规则：一是余额的每一次增减都由一条原子 UPDATE 完成，
 * 扣减把"余额够不够"写进 {@code WHERE}，判断与写入是同一条 SQL；二是每一类变动都在流水表留下
 * 带 {@code orderSn} 与 {@code changeType} 的一行，唯一索引 {@code uk_order_change} 让重投只落账一次；
 * 三是退回锚定在抵扣流水上，没有抵扣过的订单退不进来。
 */
@Slf4j
@Service("integrationAccountService")
public class IntegrationAccountServiceImpl implements IntegrationAccountService {

    /** 流水来源：购物。三类订单变动都由购物产生，取值沿用建表注释里的 [0-购物]。 */
    private static final int SOURCE_TYPE_SHOPPING = 0;

    /** 会员数据访问，读写积分余额。 */
    private final MemberDao memberDao;
    /** 积分流水查询，用于幂等判重与落账。 */
    private final IntegrationChangeHistoryService integrationChangeHistoryService;

    /**
     * 构造服务，依赖由容器注入，创建后可直接使用。
     *
     * @param memberDao 会员数据访问，不能为 {@code null}
     * @param integrationChangeHistoryService 积分流水查询，不能为 {@code null}
     */
    public IntegrationAccountServiceImpl(MemberDao memberDao,
                                         IntegrationChangeHistoryService integrationChangeHistoryService) {
        this.memberDao = memberDao;
        this.integrationChangeHistoryService = integrationChangeHistoryService;
    }

    /** {@inheritDoc} */
    @Override
    public int balance(Long memberId) {
        MemberEntity member = memberDao.selectById(memberId);
        if (member == null) {
            throw new BaseException(BaseCodeEnum.MEMBER_NOT_FOUND);
        }

        // 积分列允许为 null，对外一律归一到 0：调用方不必为"从没得过积分"再写一个分支
        return member.getIntegration() == null ? 0 : member.getIntegration();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deduct(Long memberId, String orderSn, int points) {
        change(memberId, orderSn, IntegrationChangeTypeEnum.ORDER_USE, points);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refund(Long memberId, String orderSn, int points) {
        change(memberId, orderSn, IntegrationChangeTypeEnum.ORDER_REFUND, points);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grant(Long memberId, String orderSn, int points) {
        change(memberId, orderSn, IntegrationChangeTypeEnum.ORDER_GRANT, points);
    }

    /**
     * 按变动类型增减余额，并落一条对应的流水。
     *
     * <p>幂等有两道：先按 {@code orderSn + changeType} 查流水，命中就直接返回 —— 消息重投走这条，
     * 既不报错也不重复入账；并发下两次请求都越过这次查询时，由唯一索引 {@code uk_order_change} 拦下
     * 后一次，异常抛出使事务把该次余额增减一并回滚。唯一键异常刻意不在这里捕获：吞掉它会让事务继续
     * 提交，于是"流水没有、余额却改了"的账就留在库里。
     *
     * <p>顺序是先改余额再落流水：可能失败的那一步放在后面，失败时回滚掉的就是本次全部改动。
     *
     * <p>退回在找不到对应抵扣流水时按无操作处理，不抛出。
     *
     * @param memberId 会员 id，不能为 {@code null}
     * @param orderSn 订单号，不能为 {@code null}
     * @param type 变动类型，不能为 {@code null}
     * @param points 变动数量，必须为正数
     * @throws BaseException 数量非正（10001）、会员不存在（15002）或余额不足（15009）时抛出
     */
    private void change(Long memberId, String orderSn, IntegrationChangeTypeEnum type, int points) {
        if (points <= 0) {
            throw new BaseException(BaseCodeEnum.VALID_EXCEPTION, "积分变动数量必须大于 0");
        }

        // 退回必须锚定在"确实抵扣过"上：本方法是按订单号无条件入账的，拿一个没抵扣过的订单号
        // 调进来就能凭空加积分。找不到抵扣流水时按无操作返回，与券的退回"找不到占用中的券就什么都不做"
        // 同一口径 —— 这样调用方也可以放心地在调用对方之前就登记回滚补偿
        if (type == IntegrationChangeTypeEnum.ORDER_REFUND && !hasLedger(orderSn, IntegrationChangeTypeEnum.ORDER_USE)) {
            log.warn("该订单没有抵扣过积分，退回按无操作处理：orderSn={}", orderSn);
            return;
        }

        // 幂等：这一次变动已经落过账就直接返回。消息重投走这条，不能报错 ——
        // 报错会让支付回调或关单消息把一件已经做成的事反复重试
        if (hasLedger(orderSn, type)) {
            log.info("积分变动已落账，按幂等返回：memberId={}, orderSn={}, changeType={}",
                    memberId, orderSn, type.getCode());
            return;
        }

        int affected = type.isIncrease()
                ? memberDao.addIntegration(memberId, points)
                : memberDao.deductIntegration(memberId, points);
        if (affected == 0) {
            // 扣减的条件没命中时回读一次：分清"会员不存在"与"余额不足"，
            // 这两种情况给会员看的文案与排查方向完全不同
            if (memberDao.selectById(memberId) == null) {
                throw new BaseException(BaseCodeEnum.MEMBER_NOT_FOUND);
            }
            throw new BaseException(BaseCodeEnum.INTEGRATION_NOT_ENOUGH);
        }

        IntegrationChangeHistoryEntity history = new IntegrationChangeHistoryEntity();
        history.setMemberId(memberId);
        history.setCreateTime(new Date());
        history.setChangeCount(type.isIncrease() ? points : -points);
        history.setOrderSn(orderSn);
        history.setChangeType(type.getCode());
        history.setNote(type.getMsg());
        history.setSourceType(SOURCE_TYPE_SHOPPING);
        integrationChangeHistoryService.save(history);
    }

    /**
     * 判断某一单的某类变动是否已经落过账。
     *
     * @param orderSn 订单号，不能为 {@code null}
     * @param type 变动类型，不能为 {@code null}
     * @return {@code true} 表示流水表里已有这一类变动的行
     */
    private boolean hasLedger(String orderSn, IntegrationChangeTypeEnum type) {
        return integrationChangeHistoryService.getOne(
                new LambdaQueryWrapper<IntegrationChangeHistoryEntity>()
                        .eq(IntegrationChangeHistoryEntity::getOrderSn, orderSn)
                        .eq(IntegrationChangeHistoryEntity::getChangeType, type.getCode())) != null;
    }
}

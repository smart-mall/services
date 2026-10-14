package member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import common.exception.BaseCodeEnum;
import common.exception.BaseException;
import common.to.MemberGrowthVo;
import common.to.MemberLevelVo;
import lombok.extern.slf4j.Slf4j;
import member.dao.MemberDao;
import member.dao.MemberLevelDao;
import member.entity.GrowthChangeHistoryEntity;
import member.entity.MemberEntity;
import member.entity.MemberLevelEntity;
import member.enums.GrowthChangeTypeEnum;
import member.service.GrowthAccountService;
import member.service.GrowthChangeHistoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 会员成长值账户的变动实现：支付赠送、评价赠送、等级重算与成长状态查询。
 *
 * <p>无状态、线程安全。三条贯穿全类的规则：一是成长值的每一次增减都由一条原子 UPDATE 完成；
 * 二是每一类变动都在流水表留下带 {@code orderSn} 与 {@code changeType} 的一行，唯一索引
 * {@code uk_order_change} 让重投只落账一次；三是等级在成长值累加后同事务重算，且只升不降 ——
 * 后台把门槛调高不会把已经拿到等级的会员降回去。
 */
@Slf4j
@Service("growthAccountService")
public class GrowthAccountServiceImpl implements GrowthAccountService {

    /** 流水来源：购物。两类订单变动都由购物产生，取值沿用建表注释里的 [0-购物]。 */
    private static final int SOURCE_TYPE_SHOPPING = 0;

    /** 会员数据访问，读写成长值与等级归属。 */
    private final MemberDao memberDao;
    /** 会员等级查询，用于重算等级与取每次评价的成长值。 */
    private final MemberLevelDao memberLevelDao;
    /** 成长值流水查询，用于幂等判重与落账。 */
    private final GrowthChangeHistoryService growthChangeHistoryService;

    /**
     * 构造服务，依赖由容器注入，创建后可直接使用。
     *
     * @param memberDao 会员数据访问，不能为 {@code null}
     * @param memberLevelDao 会员等级查询，不能为 {@code null}
     * @param growthChangeHistoryService 成长值流水查询，不能为 {@code null}
     */
    public GrowthAccountServiceImpl(MemberDao memberDao, MemberLevelDao memberLevelDao,
                                    GrowthChangeHistoryService growthChangeHistoryService) {
        this.memberDao = memberDao;
        this.memberLevelDao = memberLevelDao;
        this.growthChangeHistoryService = growthChangeHistoryService;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grant(Long memberId, String orderSn, int points) {
        change(memberId, orderSn, GrowthChangeTypeEnum.ORDER_GRANT, points);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grantByComment(Long memberId, String orderSn) {
        // 先判幂等再解析等级配置：重投的评价不该因为等级后来被改过而改变这一次的判定
        if (hasLedger(orderSn, GrowthChangeTypeEnum.COMMENT_GRANT)) {
            log.info("成长值变动已落账，按幂等返回：memberId={}, orderSn={}, changeType={}",
                    memberId, orderSn, GrowthChangeTypeEnum.COMMENT_GRANT.getCode());
            return;
        }

        MemberEntity member = memberDao.selectById(memberId);
        if (member == null) {
            throw new BaseException(BaseCodeEnum.MEMBER_NOT_FOUND);
        }
        MemberLevelEntity level = member.getLevelId() == null ? null : memberLevelDao.selectById(member.getLevelId());
        Integer points = level == null ? null : level.getCommentGrowthPoint();

        // 会员没有等级、或该等级把每次评价成长值配成 0 时按无操作返回：两者都是合法配置，
        // 抛错会把评价提交整个挡住，而少给成长值只是少给
        if (points == null || points <= 0) {
            log.warn("本次评价没有可送的成长值，按无操作处理：memberId={}, orderSn={}, levelId={}, commentGrowthPoint={}",
                    memberId, orderSn, member.getLevelId(), points);
            return;
        }

        change(memberId, orderSn, GrowthChangeTypeEnum.COMMENT_GRANT, points);
    }

    /** {@inheritDoc} */
    @Override
    public MemberGrowthVo snapshot(Long memberId) {
        MemberEntity member = memberDao.selectById(memberId);
        if (member == null) {
            throw new BaseException(BaseCodeEnum.MEMBER_NOT_FOUND);
        }

        MemberGrowthVo vo = new MemberGrowthVo();
        // 成长值列允许为 null，对外一律归一到 0：调用方不必为"从没得过成长值"再写一个分支
        vo.setGrowth(member.getGrowth() == null ? 0 : member.getGrowth());

        MemberLevelEntity current = member.getLevelId() == null ? null : memberLevelDao.selectById(member.getLevelId());
        if (current != null) {
            vo.setLevelId(current.getId());
            vo.setLevelName(current.getName());
            vo.setGrowthPoint(current.getGrowthPoint());
            // 等级权益只认 1，其余（0 与未配置）都按没有该权益处理
            vo.setFreeFreight(Integer.valueOf(1).equals(current.getPriviledgeFreeFreight()));
        }

        // 下一级取门槛刚好高过当前等级的那一档。按存储的等级算而不是按成长值算：
        // 等级只升不降，门槛被调高时会员仍留在原等级，按成长值算会把下一级算成他已经离开的那一档
        for (MemberLevelEntity level : listLevelEntities()) {
            if (level.getGrowthPoint() != null && isHigherThanCurrent(level, current)) {
                vo.setNextLevelName(level.getName());
                vo.setNextGrowthPoint(level.getGrowthPoint());
                break;
            }
        }
        return vo;
    }

    /** {@inheritDoc} */
    @Override
    public List<MemberLevelVo> listLevels() {
        return listLevelEntities().stream().map(level -> {
            MemberLevelVo vo = new MemberLevelVo();
            vo.setId(level.getId());
            vo.setName(level.getName());
            vo.setGrowthPoint(level.getGrowthPoint());
            return vo;
        }).toList();
    }

    /**
     * 按变动类型累加成长值，重算等级，并落一条对应的流水。
     *
     * <p>幂等有两道：先按 {@code orderSn + changeType} 查流水，命中就直接返回 —— 消息重投走这条，
     * 既不报错也不重复累加；并发下两次请求都越过这次查询时，由唯一索引 {@code uk_order_change}
     * 拦下后一次，异常抛出使事务把该次成长值累加一并回滚。唯一键异常刻意不在这里捕获：吞掉它会让事务
     * 继续提交，于是"流水没有、成长值却涨了"的账就留在库里。
     *
     * <p>顺序是先改成长值再落流水：可能失败的那一步放在后面，失败时回滚掉的就是本次全部改动。
     *
     * @param memberId 会员 id，不能为 {@code null}
     * @param orderSn 订单号，不能为 {@code null}
     * @param type 变动类型，不能为 {@code null}
     * @param points 变动数量，必须为正数
     * @throws BaseException 数量非正（10001）或会员不存在（15002）时抛出
     */
    private void change(Long memberId, String orderSn, GrowthChangeTypeEnum type, int points) {
        if (points <= 0) {
            throw new BaseException(BaseCodeEnum.VALID_EXCEPTION, "成长值变动数量必须大于 0");
        }

        // 幂等：这一次变动已经落过账就直接返回。消息重投走这条，不能报错 ——
        // 报错会让支付回调或评价重试把一件已经做成的事反复重做
        if (hasLedger(orderSn, type)) {
            log.info("成长值变动已落账，按幂等返回：memberId={}, orderSn={}, changeType={}",
                    memberId, orderSn, type.getCode());
            return;
        }

        if (memberDao.addGrowth(memberId, points) == 0) {
            throw new BaseException(BaseCodeEnum.MEMBER_NOT_FOUND);
        }

        // 等级与成长值必须同一事务：分两步做的话，中间态会让会员拿着新成长值卡在旧等级上，
        // 而等级决定他能领哪些券
        refreshLevel(memberId);

        GrowthChangeHistoryEntity history = new GrowthChangeHistoryEntity();
        history.setMemberId(memberId);
        history.setCreateTime(new Date());
        history.setChangeCount(points);
        history.setOrderSn(orderSn);
        history.setChangeType(type.getCode());
        history.setNote(type.getMsg());
        history.setSourceType(SOURCE_TYPE_SHOPPING);
        growthChangeHistoryService.save(history);
    }

    /**
     * 按最新的成长值重算会员等级，只升不降。
     *
     * <p>成长值只有增加来源，等级因此不会自然回落；这里额外挡一次"门槛被后台调高"的情况 ——
     * 已经拿到等级的会员不该因为规则改了就掉档。
     *
     * @param memberId 会员 id，不能为 {@code null}
     */
    private void refreshLevel(Long memberId) {
        MemberEntity member = memberDao.selectById(memberId);
        if (member == null) {
            return;
        }

        MemberLevelEntity target = highestReachedLevel(member.getGrowth() == null ? 0 : member.getGrowth());
        if (target == null || target.getId().equals(member.getLevelId())) {
            return;
        }

        MemberLevelEntity current = member.getLevelId() == null ? null : memberLevelDao.selectById(member.getLevelId());
        // 比门槛而不是比等级主键：主键大小与等级高低同序只是当前数据的巧合
        if (current != null && current.getGrowthPoint() != null && target.getGrowthPoint() != null
                && target.getGrowthPoint() <= current.getGrowthPoint()) {
            return;
        }

        memberDao.updateLevel(memberId, target.getId());
    }

    /**
     * 找出成长值够得着的最高等级。
     *
     * @param growth 当前的成长值累计值
     * @return 门槛不超过 {@code growth} 的最高等级；一个都够不着时返回 {@code null}
     */
    private MemberLevelEntity highestReachedLevel(int growth) {
        MemberLevelEntity target = null;
        for (MemberLevelEntity level : listLevelEntities()) {
            // 门槛为 null 的等级不参与判定：没有可比较的依据，够不够得着无从谈起
            if (level.getGrowthPoint() != null && growth >= level.getGrowthPoint()) {
                target = level;
            }
        }
        return target;
    }

    /**
     * 判断某个等级是否高于会员当前所在的等级。
     *
     * <p>比门槛而不是比等级主键：主键大小与等级高低同序只是当前数据的巧合。
     * 会员没有等级或当前等级没有门槛时，任何有门槛的等级都算"更高"。
     *
     * @param level 待比较的等级，不能为 {@code null}
     * @param current 会员当前等级，可以为 {@code null}
     * @return {@code true} 表示 {@code level} 比当前等级高
     */
    private boolean isHigherThanCurrent(MemberLevelEntity level, MemberLevelEntity current) {
        return current == null || current.getGrowthPoint() == null
                || level.getGrowthPoint() > current.getGrowthPoint();
    }

    /**
     * 取全部等级，按成长值门槛升序。
     *
     * @return 等级列表；等级表为空时为空列表
     */
    private List<MemberLevelEntity> listLevelEntities() {
        return memberLevelDao.selectList(new LambdaQueryWrapper<MemberLevelEntity>()
                .orderByAsc(MemberLevelEntity::getGrowthPoint));
    }

    /**
     * 判断某一单的某类变动是否已经落过账。
     *
     * @param orderSn 订单号，不能为 {@code null}
     * @param type 变动类型，不能为 {@code null}
     * @return {@code true} 表示流水表里已有这一类变动的行
     */
    private boolean hasLedger(String orderSn, GrowthChangeTypeEnum type) {
        return growthChangeHistoryService.getOne(
                new LambdaQueryWrapper<GrowthChangeHistoryEntity>()
                        .eq(GrowthChangeHistoryEntity::getOrderSn, orderSn)
                        .eq(GrowthChangeHistoryEntity::getChangeType, type.getCode())) != null;
    }
}

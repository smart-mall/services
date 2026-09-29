package coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.exception.BaseCodeEnum;
import common.exception.BaseException;
import common.query.KeyPageQuery;
import common.utils.R;
import common.vo.MemberResponseVo;
import common.vo.PageVO;
import coupon.dao.CouponDao;
import coupon.entity.CouponEntity;
import coupon.entity.CouponHistoryEntity;
import coupon.entity.CouponSpuCategoryRelationEntity;
import coupon.entity.CouponSpuRelationEntity;
import coupon.enums.CouponStatusEnum;
import coupon.feign.MemberFeignService;
import coupon.service.CouponHistoryService;
import coupon.service.CouponService;
import coupon.service.CouponSpuCategoryRelationService;
import coupon.service.CouponSpuRelationService;
import coupon.vo.CouponReceivableVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 优惠券模板的服务实现：分页查询、新增、修改、删除、发布与停发，以及发券（后台定向发与会员主动领）。
 *
 * <p>无状态、线程安全。依赖的四个 service 都由构造器注入，不持有连接或线程。
 *
 * <p>三条贯穿全类的规则：一是券的统计计数（{@code publish}、{@code receiveCount}、
 * {@code useCount}）只由服务端维护，管理端提交的值一律丢弃；二是已有会员领取后模板的关键字段
 * 整体锁定，因为领取记录只存 {@code coupon_id}、不存券面权益；三是后台发券与会员主动领取共用
 * 同一套闸门，改判定时必须两条路径一起改。
 */
@Service("couponService")
@Slf4j
public class CouponServiceImpl extends ServiceImpl<CouponDao, CouponEntity> implements CouponService {

    /** 适用范围：指定分类。 */
    private static final int USE_TYPE_CATEGORY = 1;
    /** 适用范围：指定商品。 */
    private static final int USE_TYPE_SPU = 2;
    /** 领取方式：后台赠送。 */
    private static final int GET_TYPE_GRANT = 0;
    /** 领取方式：会员主动领取。 */
    private static final int GET_TYPE_RECEIVE = 1;
    /** 使用状态：未使用。 */
    private static final int USE_STATUS_UNUSED = 0;

    /** 领取记录，用于删除拦截、限领判定与定向发券写入。 */
    private final CouponHistoryService couponHistoryService;
    /** 指定商品的范围关联。 */
    private final CouponSpuRelationService couponSpuRelationService;
    /** 指定分类的范围关联。 */
    private final CouponSpuCategoryRelationService couponSpuCategoryRelationService;
    /** 会员昵称查询，只用于给领取记录补展示用的名字。 */
    private final MemberFeignService memberFeignService;

    /**
     * 构造服务，依赖由容器注入，创建后可直接使用。
     *
     * @param couponHistoryService 领取记录服务，不能为 {@code null}
     * @param couponSpuRelationService 指定商品的范围关联服务，不能为 {@code null}
     * @param couponSpuCategoryRelationService 指定分类的范围关联服务，不能为 {@code null}
     * @param memberFeignService 会员昵称查询，不能为 {@code null}
     */
    public CouponServiceImpl(CouponHistoryService couponHistoryService,
                             CouponSpuRelationService couponSpuRelationService,
                             CouponSpuCategoryRelationService couponSpuCategoryRelationService,
                             MemberFeignService memberFeignService) {
        this.couponHistoryService = couponHistoryService;
        this.couponSpuRelationService = couponSpuRelationService;
        this.couponSpuCategoryRelationService = couponSpuCategoryRelationService;
        this.memberFeignService = memberFeignService;
    }

    /** {@inheritDoc} */
    @Override
    public PageVO<CouponEntity> queryPage(KeyPageQuery query) {
        String key = query.getKey();
        LambdaQueryWrapper<CouponEntity> wrapper = new LambdaQueryWrapper<>();

        if (key != null && !key.isEmpty()) {
            // 整体括起来：不加括号时后续再补条件会被 or 拆散，变成"或"掉全部筛选
            wrapper.and(inner -> inner.like(CouponEntity::getCouponName, key)
                    .or()
                    .like(CouponEntity::getId, key));
        }

        IPage<CouponEntity> page = this.page(query.toPage(), wrapper);
        fillStatus(page.getRecords());

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

    /** {@inheritDoc} */
    @Override
    public CouponEntity getDetail(Long id) {
        CouponEntity coupon = this.getById(id);
        if (coupon == null) {
            return null;
        }

        fillRelations(coupon);
        return coupon;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveCoupon(CouponEntity coupon) {
        // 1. 跨字段校验：单字段的范围约束已由实体上的注解完成
        validateTimeRange(coupon);
        validateEnableStart(coupon, null);
        validateScope(coupon);

        // 2. 计数与发布状态由服务端决定，忽略入参：新建的券一律是未发布的草稿
        coupon.setId(null);
        coupon.setPublish(0);
        coupon.setReceiveCount(0);
        coupon.setUseCount(0);
        // 会员等级体系未接通，写入侧统一存 0，避免存下一个永远匹配不上的值
        coupon.setMemberLevel(0);

        this.save(coupon);

        // 3. 关联明细与券同一事务：否则会出现"券在但适用范围没了"
        saveRelations(coupon);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCoupon(CouponEntity coupon) {
        CouponEntity existing = coupon.getId() == null ? null : this.getById(coupon.getId());
        if (existing == null) {
            throw new BaseException(BaseCodeEnum.COUPON_NOT_FOUND);
        }

        validateTimeRange(coupon);
        validateEnableStart(coupon, existing);
        validateScope(coupon);

        // 已领出去的券只存了 coupon_id，改模板会静默改掉存量券的权益，所以关键字段整体锁定
        if (hasReceived(existing) && keyFieldsChanged(existing, coupon)) {
            throw new BaseException(BaseCodeEnum.COUPON_TEMPLATE_LOCKED);
        }

        // 这三个字段各有自己的维护链路，不接受管理端提交的值
        coupon.setPublish(existing.getPublish());
        coupon.setReceiveCount(existing.getReceiveCount());
        coupon.setUseCount(existing.getUseCount());
        coupon.setMemberLevel(0);

        this.updateById(coupon);

        // 范围关联全量替换：先清后插，避免切换范围类型后两套关联并存
        removeRelations(coupon.getId());
        saveRelations(coupon);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCoupons(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        // 判据是"谁的列里存着谁的 id"：领取记录存了 coupon_id，所以有记录就整批拦下
        long received = couponHistoryService.count(new LambdaQueryWrapper<CouponHistoryEntity>()
                .in(CouponHistoryEntity::getCouponId, ids));
        if (received > 0) {
            throw new BaseException(BaseCodeEnum.COUPON_HAS_HISTORY);
        }

        // 关联明细是券自己的从属行，跟着券一起清
        couponSpuRelationService.remove(new LambdaQueryWrapper<CouponSpuRelationEntity>()
                .in(CouponSpuRelationEntity::getCouponId, ids));
        couponSpuCategoryRelationService.remove(new LambdaQueryWrapper<CouponSpuCategoryRelationEntity>()
                .in(CouponSpuCategoryRelationEntity::getCouponId, ids));

        this.removeByIds(ids);
    }

    /** {@inheritDoc} */
    @Override
    public void publish(Long id) {
        CouponEntity coupon = this.getById(id);
        if (coupon == null) {
            throw new BaseException(BaseCodeEnum.COUPON_NOT_FOUND);
        }

        // 条件不满足的券发布出去也没人领得走，宁可在这里拦住而不是让它静静地领不掉
        boolean invalid = coupon.getAmount() == null || coupon.getAmount().compareTo(BigDecimal.ZERO) <= 0
                || coupon.getPublishCount() == null || coupon.getPublishCount() <= 0
                || coupon.getStartTime() == null || coupon.getEndTime() == null
                || coupon.getEnableStartTime() == null || coupon.getEnableEndTime() == null
                || coupon.getEndTime().before(new Date());
        if (invalid) {
            throw new BaseException(BaseCodeEnum.COUPON_PUBLISH_INVALID);
        }
        if (countRelations(id) == 0 && !isAllScope(coupon)) {
            throw new BaseException(BaseCodeEnum.COUPON_SCOPE_REQUIRED);
        }

        updatePublish(id, 1);
    }

    /** {@inheritDoc} */
    @Override
    public void revoke(Long id) {
        if (this.getById(id) == null) {
            throw new BaseException(BaseCodeEnum.COUPON_NOT_FOUND);
        }

        updatePublish(id, 0);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int grant(Long couponId, List<Long> memberIds) {
        CouponEntity coupon = this.getById(couponId);
        if (coupon == null) {
            throw new BaseException(BaseCodeEnum.COUPON_NOT_FOUND);
        }
        if (memberIds == null || memberIds.isEmpty()) {
            return 0;
        }

        Date now = new Date();
        assertReceivable(coupon, now);
        assertMemberLevel(coupon);

        // 昵称只用于展示，查不到不该挡住发券，所以这里失败不抛异常、留空继续
        Map<Long, String> nicknames = fetchMemberNicknames(memberIds);

        int perLimit = coupon.getPerLimit() == null ? 0 : coupon.getPerLimit();
        int granted = 0;
        for (Long memberId : memberIds) {
            if (memberId == null) {
                continue;
            }
            // 每人限领：先数已领张数。后台发券是低频操作，这里不做更强的并发保护
            if (countMemberReceived(couponId, memberId) >= perLimit) {
                continue;
            }
            if (!consumeOne(couponId)) {
                // 余量已尽，后面的会员也不可能成功
                break;
            }

            couponHistoryService.save(buildHistory(couponId, memberId, nicknames.get(memberId), GET_TYPE_GRANT, now));
            granted++;
        }
        return granted;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void receive(Long couponId, MemberResponseVo user) {
        CouponEntity coupon = this.getById(couponId);
        if (coupon == null) {
            throw new BaseException(BaseCodeEnum.COUPON_NOT_FOUND);
        }

        Date now = new Date();
        assertReceivable(coupon, now);
        assertMemberLevel(coupon);

        // 顺序不能颠倒：先扣发行量拿到 sms_coupon 这一行的排他锁，同一张券的领取请求才被串行化，
        // 下面那次限领计数读到的才是稳定值。先数后扣的话，两个并发请求会各自数到旧值、双双放行
        if (!consumeOne(couponId)) {
            throw new BaseException(BaseCodeEnum.COUPON_SOLD_OUT);
        }

        Long memberId = user.getId();
        int perLimit = coupon.getPerLimit() == null ? 0 : coupon.getPerLimit();
        if (countMemberReceived(couponId, memberId) >= perLimit) {
            // 抛异常由事务把上面那次扣减一并回滚，不会白吃掉一个发行额度
            throw new BaseException(BaseCodeEnum.COUPON_RECEIVE_LIMIT_EXCEEDED);
        }

        // 昵称直接取网关注入的身份，不必再回查会员服务
        couponHistoryService.save(buildHistory(couponId, memberId, user.getNickname(), GET_TYPE_RECEIVE, now));
    }

    /** {@inheritDoc} */
    @Override
    public List<CouponReceivableVo> listReceivable(Long memberId) {
        Date now = new Date();
        List<CouponEntity> coupons = this.list(new LambdaQueryWrapper<CouponEntity>()
                // 未发布、不在领取窗口内、限会员等级的都不进券中心：领了也过不了领取闸门
                .eq(CouponEntity::getPublish, 1)
                .le(CouponEntity::getEnableStartTime, now)
                .gt(CouponEntity::getEnableEndTime, now)
                .and(inner -> inner.isNull(CouponEntity::getMemberLevel)
                        .or()
                        .eq(CouponEntity::getMemberLevel, 0))
                // 面额大的排前面：会员进券中心第一眼想看到的是最值钱的那张
                .orderByDesc(CouponEntity::getAmount));
        if (coupons.isEmpty()) {
            return List.of();
        }

        List<CouponEntity> inStock = coupons.stream()
                .filter(coupon -> remainCount(coupon) > 0)
                .toList();
        if (inStock.isEmpty()) {
            return List.of();
        }

        // 会员已领张数按券一次查完再分组：逐张查会变成"券中心有几张券就发几次查询"
        Map<Long, Long> receivedByCouponId = countMemberReceived(
                inStock.stream().map(CouponEntity::getId).toList(), memberId);

        return inStock.stream().map(coupon -> {
            CouponReceivableVo vo = new CouponReceivableVo();
            vo.setCouponId(coupon.getId());
            vo.setCouponName(coupon.getCouponName());
            vo.setAmount(coupon.getAmount());
            vo.setMinPoint(coupon.getMinPoint());
            vo.setUseType(coupon.getUseType());
            vo.setEndTime(coupon.getEndTime());
            vo.setEnableEndTime(coupon.getEnableEndTime());
            vo.setRemainCount(remainCount(coupon));
            vo.setReceivedCount(receivedByCouponId.getOrDefault(coupon.getId(), 0L).intValue());
            vo.setPerLimit(coupon.getPerLimit());
            return vo;
        }).toList();
    }

    /* ═══════════════════ 内部 ═══════════════════ */

    /**
     * 算一张券还剩多少可领。
     *
     * @param coupon 券，不能为 {@code null}
     * @return 发行总量减已领张数；两个计数为空时按 0 处理，余量不为负
     */
    private int remainCount(CouponEntity coupon) {
        int publishCount = coupon.getPublishCount() == null ? 0 : coupon.getPublishCount();
        int receiveCount = coupon.getReceiveCount() == null ? 0 : coupon.getReceiveCount();

        return Math.max(0, publishCount - receiveCount);
    }

    /**
     * 批量统计某会员对一批券各已领多少张。
     *
     * @param couponIds 券模板主键列表，不能为 {@code null}，可以为空集合
     * @param memberId 会员主键，不能为 {@code null}
     * @return 券模板主键到已领张数的映射；没领过的券不在映射中
     */
    private Map<Long, Long> countMemberReceived(List<Long> couponIds, Long memberId) {
        // 必须先判空：空集合会让 SQL 拼成 IN ()，MySQL 直接报语法错
        if (couponIds.isEmpty()) {
            return Map.of();
        }
        return couponHistoryService.list(new LambdaQueryWrapper<CouponHistoryEntity>()
                        .in(CouponHistoryEntity::getCouponId, couponIds)
                        .eq(CouponHistoryEntity::getMemberId, memberId))
                .stream()
                .collect(Collectors.groupingBy(CouponHistoryEntity::getCouponId, Collectors.counting()));
    }

    /**
     * 为列表回填现算的状态与可执行动作。
     *
     * @param coupons 券列表，不能为 {@code null}，可以为空
     */
    private void fillStatus(List<CouponEntity> coupons) {
        // 同一次查询共用一个判定时间，避免跨行状态因时间漂移而不一致
        Date now = new Date();
        for (CouponEntity coupon : coupons) {
            coupon.setStatusText(CouponStatusEnum.resolve(coupon, now).getMsg());
            coupon.setAllowedActions(CouponStatusEnum.allowedActions(coupon, now));
        }
    }

    /**
     * 按适用范围类型带出关联明细，不适用的一侧给空列表，让前端拿到的永远是数组。
     *
     * @param coupon 券，不能为 {@code null}
     */
    private void fillRelations(CouponEntity coupon) {
        boolean byCategory = Integer.valueOf(USE_TYPE_CATEGORY).equals(coupon.getUseType());
        boolean bySpu = Integer.valueOf(USE_TYPE_SPU).equals(coupon.getUseType());

        coupon.setCategoryRelations(byCategory
                ? couponSpuCategoryRelationService.list(new LambdaQueryWrapper<CouponSpuCategoryRelationEntity>()
                        .eq(CouponSpuCategoryRelationEntity::getCouponId, coupon.getId()))
                : List.of());
        coupon.setSpuRelations(bySpu
                ? couponSpuRelationService.list(new LambdaQueryWrapper<CouponSpuRelationEntity>()
                        .eq(CouponSpuRelationEntity::getCouponId, coupon.getId()))
                : List.of());
    }

    /**
     * 校验两个时间区间，并保证有效期覆盖到领取窗口结束之后。
     *
     * @param coupon 券，不能为 {@code null}
     * @throws BaseException 任一时间为空，或有效期结束早于领取结束时抛出
     */
    private void validateTimeRange(CouponEntity coupon) {
        // 领到手就已过期的券没有意义：有效期至少要覆盖到领取窗口结束
        if (coupon.getStartTime() == null || coupon.getEndTime() == null
                || coupon.getEnableStartTime() == null || coupon.getEnableEndTime() == null) {
            throw new BaseException(BaseCodeEnum.COUPON_TIME_RANGE_INVALID);
        }
        if (coupon.getEndTime().before(coupon.getEnableEndTime())) {
            throw new BaseException(BaseCodeEnum.COUPON_TIME_RANGE_INVALID);
        }
    }

    /**
     * 校验领取开始时间不早于当前时间。
     *
     * <p>只在新建、或本次提交改动了该字段时校验：券的领取窗口一旦已经开始，
     * 改个券名不该被一个已经过去的时刻挡住。
     *
     * @param coupon 本次提交的券，不能为 {@code null}
     * @param existing 库里的券；新建时为 {@code null}
     * @throws BaseException 领取开始时间早于当前时间时抛出
     */
    private void validateEnableStart(CouponEntity coupon, CouponEntity existing) {
        if (coupon.getEnableStartTime() == null) {
            return;
        }
        if (existing != null && existing.getEnableStartTime() != null
                && existing.getEnableStartTime().equals(coupon.getEnableStartTime())) {
            return;
        }
        // 提交的时间只精确到秒、当前时间带毫秒，直接比会把"就选此刻"判成过去，所以截到分钟
        long nowFloorToMinute = System.currentTimeMillis() / 60000 * 60000;
        if (coupon.getEnableStartTime().getTime() < nowFloorToMinute) {
            throw new BaseException(BaseCodeEnum.COUPON_ENABLE_START_IN_PAST);
        }
    }

    /**
     * 校验指定范围时关联明细非空。
     *
     * @param coupon 券，不能为 {@code null}
     * @throws BaseException 选了指定分类或指定商品却没有对应明细时抛出
     */
    private void validateScope(CouponEntity coupon) {
        boolean emptyCategories = coupon.getCategoryRelations() == null || coupon.getCategoryRelations().isEmpty();
        boolean emptySpus = coupon.getSpuRelations() == null || coupon.getSpuRelations().isEmpty();

        if (Integer.valueOf(USE_TYPE_CATEGORY).equals(coupon.getUseType()) && emptyCategories) {
            throw new BaseException(BaseCodeEnum.COUPON_SCOPE_REQUIRED);
        }
        if (Integer.valueOf(USE_TYPE_SPU).equals(coupon.getUseType()) && emptySpus) {
            throw new BaseException(BaseCodeEnum.COUPON_SCOPE_REQUIRED);
        }
    }

    /**
     * 写入适用范围关联明细，按适用范围类型只写对应的一侧。
     *
     * @param coupon 券，其 {@code id} 必须已有值，不能为 {@code null}
     */
    private void saveRelations(CouponEntity coupon) {
        if (Integer.valueOf(USE_TYPE_CATEGORY).equals(coupon.getUseType())) {
            for (CouponSpuCategoryRelationEntity relation : coupon.getCategoryRelations()) {
                // 清掉前端回传的主键：那是上一次加载的行，留着会更新到错误的记录上
                relation.setId(null);
                relation.setCouponId(coupon.getId());
                couponSpuCategoryRelationService.save(relation);
            }
        } else if (Integer.valueOf(USE_TYPE_SPU).equals(coupon.getUseType())) {
            for (CouponSpuRelationEntity relation : coupon.getSpuRelations()) {
                relation.setId(null);
                relation.setCouponId(coupon.getId());
                couponSpuRelationService.save(relation);
            }
        }
    }

    /**
     * 清空一张券的全部适用范围关联。
     *
     * @param couponId 优惠券主键，不能为 {@code null}
     */
    private void removeRelations(Long couponId) {
        couponSpuRelationService.remove(new LambdaQueryWrapper<CouponSpuRelationEntity>()
                .eq(CouponSpuRelationEntity::getCouponId, couponId));
        couponSpuCategoryRelationService.remove(new LambdaQueryWrapper<CouponSpuCategoryRelationEntity>()
                .eq(CouponSpuCategoryRelationEntity::getCouponId, couponId));
    }

    /**
     * 统计一张券的适用范围关联明细总数。
     *
     * @param couponId 优惠券主键，不能为 {@code null}
     * @return 分类关联与商品关联的条数之和
     */
    private long countRelations(Long couponId) {
        long categories = couponSpuCategoryRelationService.count(
                new LambdaQueryWrapper<CouponSpuCategoryRelationEntity>()
                        .eq(CouponSpuCategoryRelationEntity::getCouponId, couponId));
        long spus = couponSpuRelationService.count(new LambdaQueryWrapper<CouponSpuRelationEntity>()
                .eq(CouponSpuRelationEntity::getCouponId, couponId));

        return categories + spus;
    }

    /**
     * 判断券的适用范围是否为全场通用。
     *
     * @param coupon 券，不能为 {@code null}
     * @return {@code true} 表示不需要关联明细
     */
    private boolean isAllScope(CouponEntity coupon) {
        return !Integer.valueOf(USE_TYPE_CATEGORY).equals(coupon.getUseType())
                && !Integer.valueOf(USE_TYPE_SPU).equals(coupon.getUseType());
    }

    /**
     * 只更新发布状态，不触碰其它列。
     *
     * @param id 优惠券主键，不能为 {@code null}
     * @param publish 目标发布状态，0 为未发布、1 为已发布
     */
    private void updatePublish(Long id, int publish) {
        // 用只带主键与目标列的实体走部分更新，避免把读出来的旧计数整行写回去
        CouponEntity update = new CouponEntity();
        update.setId(id);
        update.setPublish(publish);

        this.updateById(update);
    }

    /**
     * 判断券是否已被会员领取过。
     *
     * @param coupon 券，不能为 {@code null}
     * @return {@code true} 表示已有领取记录
     */
    private boolean hasReceived(CouponEntity coupon) {
        return coupon.getReceiveCount() != null && coupon.getReceiveCount() > 0;
    }

    /**
     * 判断本次提交是否改动了领取后即锁定的字段。
     *
     * @param existing 库里的券，不能为 {@code null}
     * @param submitted 本次提交的券，不能为 {@code null}
     * @return {@code true} 表示有关键字段发生变化
     */
    private boolean keyFieldsChanged(CouponEntity existing, CouponEntity submitted) {
        return !sameAmount(existing.getAmount(), submitted.getAmount())
                || !sameAmount(existing.getMinPoint(), submitted.getMinPoint())
                || !Objects.equals(existing.getPerLimit(), submitted.getPerLimit())
                || !Objects.equals(existing.getPublishCount(), submitted.getPublishCount())
                || !Objects.equals(existing.getUseType(), submitted.getUseType())
                || !Objects.equals(existing.getStartTime(), submitted.getStartTime())
                || !Objects.equals(existing.getEndTime(), submitted.getEndTime())
                || !Objects.equals(existing.getEnableStartTime(), submitted.getEnableStartTime())
                || !Objects.equals(existing.getEnableEndTime(), submitted.getEnableEndTime());
    }

    /**
     * 比较两个金额是否等值。
     *
     * <p>必须用 {@code compareTo}：库里是 {@code 30.0000}、前端回传 {@code 30}，数值相等但
     * {@code equals} 会判不等，那样只要动一次表单就会被当成改了面额而拒绝保存。
     *
     * @param left 左值，可以为 {@code null}
     * @param right 右值，可以为 {@code null}
     * @return {@code true} 表示两者都为空或数值相等
     */
    private boolean sameAmount(BigDecimal left, BigDecimal right) {
        if (left == null || right == null) {
            return left == right;
        }
        return left.compareTo(right) == 0;
    }

    /**
     * 校验券当前处于可领取状态。
     *
     * @param coupon 券，不能为 {@code null}
     * @param now 判定基准时间，不能为 {@code null}
     * @throws BaseException 券未发布，或当前不在领取窗口内时抛出
     */
    private void assertReceivable(CouponEntity coupon, Date now) {
        boolean notPublished = !Integer.valueOf(1).equals(coupon.getPublish());
        boolean notStarted = coupon.getEnableStartTime() != null && now.before(coupon.getEnableStartTime());
        boolean ended = coupon.getEnableEndTime() != null && now.after(coupon.getEnableEndTime());

        if (notPublished || notStarted || ended) {
            throw new BaseException(BaseCodeEnum.COUPON_NOT_PUBLISHED);
        }
    }

    /**
     * 校验会员等级满足券的限定。
     *
     * <p>限等级的券在等级体系接通前一律拒绝：服务端拿不到会员等级，放行等于把限定范围作废。
     *
     * @param coupon 券，不能为 {@code null}
     * @throws BaseException 券限定了会员等级时抛出
     */
    private void assertMemberLevel(CouponEntity coupon) {
        if (coupon.getMemberLevel() != null && coupon.getMemberLevel() != 0) {
            throw new BaseException(BaseCodeEnum.COUPON_MEMBER_LEVEL_MISMATCH);
        }
    }

    /**
     * 占用一张发行额度。
     *
     * <p>判断与扣减是同一条 SQL，不存在"查的时候还有、写的时候没了"的窗口；
     * 同一张券的并发领取请求会在这一行上排队，所以它同时是限领计数的串行化点。
     *
     * @param couponId 优惠券主键，不能为 {@code null}
     * @return {@code true} 表示占用成功；余量已尽返回 {@code false}
     */
    private boolean consumeOne(Long couponId) {
        // 两个计数列都允许为 NULL，必须 COALESCE：NULL 参与比较恒为假，一张也发不出去
        return this.update(new LambdaUpdateWrapper<CouponEntity>()
                .setSql("receive_count = COALESCE(receive_count, 0) + 1")
                .eq(CouponEntity::getId, couponId)
                .apply("COALESCE(receive_count, 0) < COALESCE(publish_count, 0)"));
    }

    /**
     * 统计某会员已领取某券的张数。
     *
     * @param couponId 优惠券主键，不能为 {@code null}
     * @param memberId 会员主键，不能为 {@code null}
     * @return 已领取张数，没有记录时为 0
     */
    private long countMemberReceived(Long couponId, Long memberId) {
        return couponHistoryService.count(new LambdaQueryWrapper<CouponHistoryEntity>()
                .eq(CouponHistoryEntity::getCouponId, couponId)
                .eq(CouponHistoryEntity::getMemberId, memberId));
    }

    /**
     * 组装一条领取记录。
     *
     * @param couponId 优惠券主键，不能为 {@code null}
     * @param memberId 会员主键，不能为 {@code null}
     * @param memberNickName 会员昵称，查不到时为 {@code null}
     * @param getType 领取方式，取值见本类的 {@code GET_TYPE_*} 常量
     * @param now 领取时间，不能为 {@code null}
     * @return 待落库的领取记录
     */
    private CouponHistoryEntity buildHistory(Long couponId, Long memberId, String memberNickName,
                                             int getType, Date now) {
        CouponHistoryEntity history = new CouponHistoryEntity();
        history.setCouponId(couponId);
        history.setMemberId(memberId);
        history.setMemberNickName(memberNickName);
        // 后台赠送与用户主动领取靠 getType 区分，统计领取率时要分开看
        history.setGetType(getType);
        history.setCreateTime(now);
        history.setUseType(USE_STATUS_UNUSED);

        return history;
    }

    /**
     * 批量查会员昵称，供领取记录展示。
     *
     * <p>查询失败时返回空映射、由调用方把昵称留空，而不是让发券失败 ——
     * 昵称只是展示字段，与"删除前必须确认下游归属"那类失败关闭的判断不同，不该挡住一次真实的发券。
     *
     * @param memberIds 会员 ID 列表，不能为 {@code null}
     * @return 会员 ID 到昵称的映射；查不到时返回空映射，不返回 {@code null}
     */
    private Map<Long, String> fetchMemberNicknames(List<Long> memberIds) {
        try {
            R<Map<Long, String>> response = memberFeignService.getMemberNames(memberIds);
            if (response != null && response.getCode() == 0 && response.getData() != null) {
                return response.getData();
            }
            log.warn("查询会员昵称失败，领取记录将不带昵称，code={}",
                    response == null ? null : response.getCode());
        } catch (Exception e) {
            log.warn("查询会员昵称异常，领取记录将不带昵称", e);
        }
        return Map.of();
    }

}

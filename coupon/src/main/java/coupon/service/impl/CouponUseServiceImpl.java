package coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import common.exception.BaseCodeEnum;
import common.exception.BaseException;
import common.to.CouponCartItemVo;
import common.to.CouponUsableVo;
import common.to.SkuScopeVo;
import common.utils.R;
import common.vo.PageVO;
import coupon.dao.CouponDao;
import coupon.entity.CouponEntity;
import coupon.entity.CouponHistoryEntity;
import coupon.entity.CouponSpuCategoryRelationEntity;
import coupon.entity.CouponSpuRelationEntity;
import coupon.enums.CouponUseStatusEnum;
import coupon.feign.ProductFeignService;
import coupon.service.CouponHistoryService;
import coupon.service.CouponSpuCategoryRelationService;
import coupon.service.CouponSpuRelationService;
import coupon.service.CouponUseService;
import coupon.vo.CouponHistoryPageQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 已领到手的券的使用链路实现：查我的券、按购物车筛可用券、下单占用、付款核销、取消退回、过期清理。
 *
 * <p>无状态、线程安全。注的是 {@link CouponDao} 而不是 {@code CouponService}：后者依赖
 * {@link CouponHistoryService}，本类也用同一个 service，两边都走构造器注入会形成循环依赖。
 *
 * <p>两处贯穿全类的规则：一是"一张券"指领取记录主键而不是券模板 ID，同一会员可能持有多张同款券；
 * 二是占用、核销、退回都靠带状态条件的 UPDATE 完成，判断与写入是同一条 SQL，不存在先查后写的窗口。
 *
 * <p>券模板上的 {@code useType}（适用范围）与领取记录上的 {@code useType}（使用状态）同名不同义，
 * 本类里两者都出现，读写时必须看清楚操作的是哪个对象。
 */
@Slf4j
@Service("couponUseService")
public class CouponUseServiceImpl implements CouponUseService {

    /** 适用范围：全场通用，不需要关联明细。 */
    private static final int SCOPE_ALL = 0;
    /** 适用范围：指定分类。 */
    private static final int SCOPE_CATEGORY = 1;
    /** 适用范围：指定商品。 */
    private static final int SCOPE_SPU = 2;

    /** 过期清理每批处理的条数。 */
    private static final int EXPIRE_BATCH_SIZE = 500;

    /** 领取记录，读写券的使用状态。 */
    private final CouponHistoryService couponHistoryService;
    /** 券模板数据访问，取面额、门槛、有效期与适用范围类型。 */
    private final CouponDao couponDao;
    /** 指定分类的范围关联。 */
    private final CouponSpuCategoryRelationService couponSpuCategoryRelationService;
    /** 指定商品的范围关联。 */
    private final CouponSpuRelationService couponSpuRelationService;
    /** 商品服务，把购物车里的 skuId 换成所属 SPU 与分类。 */
    private final ProductFeignService productFeignService;

    /**
     * 构造服务，依赖由容器注入，创建后可直接使用。
     *
     * @param couponHistoryService 领取记录服务，不能为 {@code null}
     * @param couponDao 券模板数据访问，不能为 {@code null}
     * @param couponSpuCategoryRelationService 指定分类的范围关联服务，不能为 {@code null}
     * @param couponSpuRelationService 指定商品的范围关联服务，不能为 {@code null}
     * @param productFeignService 商品服务客户端，不能为 {@code null}
     */
    public CouponUseServiceImpl(CouponHistoryService couponHistoryService,
                                CouponDao couponDao,
                                CouponSpuCategoryRelationService couponSpuCategoryRelationService,
                                CouponSpuRelationService couponSpuRelationService,
                                ProductFeignService productFeignService) {
        this.couponHistoryService = couponHistoryService;
        this.couponDao = couponDao;
        this.couponSpuCategoryRelationService = couponSpuCategoryRelationService;
        this.couponSpuRelationService = couponSpuRelationService;
        this.productFeignService = productFeignService;
    }

    /** {@inheritDoc} */
    @Override
    public PageVO<CouponHistoryEntity> queryMyCoupons(Long memberId, CouponHistoryPageQuery query) {
        LambdaQueryWrapper<CouponHistoryEntity> wrapper = new LambdaQueryWrapper<CouponHistoryEntity>()
                // 会员维度由服务端强制：入参里的 memberId 一律不看，否则改个参数就能翻别人的券
                .eq(CouponHistoryEntity::getMemberId, memberId)
                .eq(query.getCouponId() != null, CouponHistoryEntity::getCouponId, query.getCouponId())
                .eq(query.getUseType() != null, CouponHistoryEntity::getUseType, query.getUseType())
                .orderByDesc(CouponHistoryEntity::getCreateTime)
                .orderByDesc(CouponHistoryEntity::getId);

        IPage<CouponHistoryEntity> page = couponHistoryService.page(query.toPage(), wrapper);
        fillCouponInfo(page.getRecords());

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

    /** {@inheritDoc} */
    @Override
    public List<CouponUsableVo> listUsable(Long memberId, List<CouponCartItemVo> items) {
        assertItemsValid(items);

        List<CouponHistoryEntity> candidates = couponHistoryService.list(
                new LambdaQueryWrapper<CouponHistoryEntity>()
                        .eq(CouponHistoryEntity::getMemberId, memberId)
                        .eq(CouponHistoryEntity::getUseType, CouponUseStatusEnum.UNUSED.getCode()));
        if (candidates.isEmpty()) {
            return List.of();
        }

        Map<Long, CouponEntity> coupons = loadCoupons(couponIdsOf(candidates));
        Map<Long, Scope> scopes = resolveScopes(items, coupons.values());
        Date now = new Date();

        List<CouponUsableVo> usable = new ArrayList<>();
        for (CouponHistoryEntity history : candidates) {
            CouponEntity coupon = coupons.get(history.getCouponId());
            if (coupon == null) {
                continue;
            }
            try {
                usable.add(evaluate(history, coupon, scopes.get(coupon.getId()), now));
            } catch (CouponNotUsableException ignored) {
                // 券不可用是正常结果：会员手里有券不等于这张券能用在这个购物车上
            }
        }

        // 抵扣多的排前面：会员端默认选第一张，排序本身就是"默认用最划算的那张"
        usable.sort(Comparator.comparing(CouponUsableVo::getDiscountAmount).reversed()
                .thenComparing(CouponUsableVo::getCouponEndTime, Comparator.nullsLast(Comparator.naturalOrder())));
        return usable;
    }

    /** {@inheritDoc} */
    @Override
    public CouponUsableVo computeDiscount(Long memberId, List<CouponCartItemVo> items, Long couponHistoryId) {
        assertItemsValid(items);

        CouponHistoryEntity history = couponHistoryService.getById(couponHistoryId);
        // 不属于当前会员时按不存在处理：回"这张券不是你的"等于告诉调用方这个 id 是有效的
        if (history == null || !Objects.equals(history.getMemberId(), memberId)) {
            throw new BaseException(BaseCodeEnum.COUPON_HISTORY_NOT_FOUND);
        }

        CouponEntity coupon = couponDao.selectById(history.getCouponId());
        if (coupon == null) {
            throw new BaseException(BaseCodeEnum.COUPON_NOT_FOUND);
        }

        Scope scope = resolveScopes(items, List.of(coupon)).get(coupon.getId());
        return evaluate(history, coupon, scope, new Date());
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void lock(Long couponHistoryId, Long memberId, Long orderId, String orderSn) {
        CouponHistoryEntity history = couponHistoryService.getById(couponHistoryId);
        if (history == null || !Objects.equals(history.getMemberId(), memberId)) {
            throw new BaseException(BaseCodeEnum.COUPON_HISTORY_NOT_FOUND);
        }

        CouponEntity coupon = couponDao.selectById(history.getCouponId());
        if (coupon == null) {
            throw new BaseException(BaseCodeEnum.COUPON_NOT_FOUND);
        }
        // 结算页与提交之间券可能刚好过期；下面那条 UPDATE 只看状态，兜不住时间
        if (outOfUsePeriod(coupon, new Date())) {
            throw new BaseException(BaseCodeEnum.COUPON_NOT_IN_USE_PERIOD);
        }

        boolean locked = couponHistoryService.update(new LambdaUpdateWrapper<CouponHistoryEntity>()
                .set(CouponHistoryEntity::getUseType, CouponUseStatusEnum.OCCUPIED.getCode())
                .set(CouponHistoryEntity::getOrderId, orderId)
                .set(CouponHistoryEntity::getOrderSn, orderSn)
                .eq(CouponHistoryEntity::getId, couponHistoryId)
                .eq(CouponHistoryEntity::getUseType, CouponUseStatusEnum.UNUSED.getCode()));
        if (locked) {
            return;
        }

        // 条件没命中才回读一次：上面的 history 是加锁前的快照，只用来把失败原因说清楚
        CouponHistoryEntity latest = couponHistoryService.getById(couponHistoryId);
        if (latest != null && Objects.equals(latest.getUseType(), CouponUseStatusEnum.OCCUPIED.getCode())) {
            throw new BaseException(BaseCodeEnum.COUPON_ALREADY_OCCUPIED);
        }
        throw new BaseException(BaseCodeEnum.COUPON_HISTORY_STATUS_INVALID);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void use(String orderSn) {
        CouponHistoryEntity occupied = findOneByOrderSn(orderSn, CouponUseStatusEnum.OCCUPIED.getCode());
        if (occupied == null) {
            // 幂等：支付回调会重复投递，已经核销过就当成功
            if (findOneByOrderSn(orderSn, CouponUseStatusEnum.USED.getCode()) != null) {
                return;
            }
            throw new BaseException(BaseCodeEnum.COUPON_HISTORY_NOT_FOUND);
        }

        boolean used = couponHistoryService.update(new LambdaUpdateWrapper<CouponHistoryEntity>()
                .set(CouponHistoryEntity::getUseType, CouponUseStatusEnum.USED.getCode())
                .set(CouponHistoryEntity::getUseTime, new Date())
                .eq(CouponHistoryEntity::getId, occupied.getId())
                .eq(CouponHistoryEntity::getUseType, CouponUseStatusEnum.OCCUPIED.getCode()));
        if (!used) {
            // 并发下已被另一次通知核销，等价于成功；计数也已由那一次加过，这里不能再加
            return;
        }

        // 已核销计数只在真正发生状态跃迁时加一：重复投递走的是上面两个 return 分支
        couponDao.update(null, new LambdaUpdateWrapper<CouponEntity>()
                .setSql("use_count = COALESCE(use_count, 0) + 1")
                .eq(CouponEntity::getId, occupied.getCouponId()));
    }

    /** {@inheritDoc} */
    @Override
    public void unlock(String orderSn) {
        CouponHistoryEntity occupied = findOneByOrderSn(orderSn, CouponUseStatusEnum.OCCUPIED.getCode());
        // 找不到就什么都不做：关单消息会重投，第二次调用、以及本来就没用券的订单都会落到这里
        if (occupied == null) {
            return;
        }

        CouponEntity coupon = couponDao.selectById(occupied.getCouponId());
        // 占用期间券已过期的，退回时直接置为已过期：置回未使用会得到一张显示可用、实际用不掉的券
        int target = coupon != null && outOfUsePeriod(coupon, new Date())
                ? CouponUseStatusEnum.EXPIRED.getCode()
                : CouponUseStatusEnum.UNUSED.getCode();

        couponHistoryService.update(new LambdaUpdateWrapper<CouponHistoryEntity>()
                .set(CouponHistoryEntity::getUseType, target)
                // 用 setSql 而不是 set(..., null)：这两列必须被显式清空，
                // 走 set 传 null 是否生成 SET 子句依赖实现细节，写死更可靠
                .setSql("order_id = NULL, order_sn = NULL")
                .eq(CouponHistoryEntity::getId, occupied.getId())
                .eq(CouponHistoryEntity::getUseType, CouponUseStatusEnum.OCCUPIED.getCode()));
    }

    /** {@inheritDoc} */
    @Override
    public int expire() {
        Date now = new Date();
        int expiredTotal = 0;
        // 游标翻页而不是 OFFSET：置为已过期的行会掉出筛选条件，OFFSET 会跳过记录
        long lastId = 0L;

        while (true) {
            List<CouponHistoryEntity> batch = couponHistoryService.list(
                    new LambdaQueryWrapper<CouponHistoryEntity>()
                            .eq(CouponHistoryEntity::getUseType, CouponUseStatusEnum.UNUSED.getCode())
                            .gt(CouponHistoryEntity::getId, lastId)
                            .orderByAsc(CouponHistoryEntity::getId)
                            .last("LIMIT " + EXPIRE_BATCH_SIZE));
            if (batch.isEmpty()) {
                break;
            }
            lastId = batch.get(batch.size() - 1).getId();

            Map<Long, CouponEntity> coupons = loadCoupons(couponIdsOf(batch));
            List<Long> expiredIds = batch.stream()
                    .filter(history -> {
                        CouponEntity coupon = coupons.get(history.getCouponId());
                        return coupon != null && coupon.getEndTime() != null && now.after(coupon.getEndTime());
                    })
                    .map(CouponHistoryEntity::getId)
                    .toList();

            if (!expiredIds.isEmpty()) {
                couponHistoryService.update(new LambdaUpdateWrapper<CouponHistoryEntity>()
                        .set(CouponHistoryEntity::getUseType, CouponUseStatusEnum.EXPIRED.getCode())
                        .in(CouponHistoryEntity::getId, expiredIds));
                expiredTotal += expiredIds.size();
            }

            if (batch.size() < EXPIRE_BATCH_SIZE) {
                break;
            }
        }

        return expiredTotal;
    }

    /* ═══════════════════ 匹配与判定 ═══════════════════ */

    /**
     * 判定一张券能不能用在给定购物车上，并算出抵扣额与适用范围内的商品。
     *
     * <p>判定顺序固定为状态、时间、范围、门槛：状态与时间是最硬的条件，
     * 先判定它们才不会为一张已经用掉的券去回查商品归属。
     *
     * <p>四条判定都以异常表达不可用，因为两条调用路径对它的反应不同：列券要逐张跳过，
     * 算价要带着原因拒绝。原因由 {@link CouponNotUsableException} 承载，两条路径各自决定怎么处理。
     *
     * @param history 领取记录，不能为 {@code null}
     * @param coupon 对应的券模板，不能为 {@code null}
     * @param scope 该券在本次购物车下的适用范围，不能为 {@code null}
     * @param now 判定基准时间，不能为 {@code null}
     * @return 该券的抵扣结果，不会返回 {@code null}
     * @throws CouponNotUsableException 券当前不能用于本次购物车时抛出，异常里带着具体原因
     */
    private CouponUsableVo evaluate(CouponHistoryEntity history, CouponEntity coupon,
                                    Scope scope, Date now) {
        if (!Objects.equals(history.getUseType(), CouponUseStatusEnum.UNUSED.getCode())) {
            throw new CouponNotUsableException(BaseCodeEnum.COUPON_HISTORY_STATUS_INVALID);
        }
        if (outOfUsePeriod(coupon, now)) {
            throw new CouponNotUsableException(BaseCodeEnum.COUPON_NOT_IN_USE_PERIOD);
        }

        BigDecimal scopeAmount = scope.amount();
        if (scopeAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CouponNotUsableException(BaseCodeEnum.COUPON_SCOPE_NOT_MATCHED);
        }
        BigDecimal minPoint = coupon.getMinPoint() == null ? BigDecimal.ZERO : coupon.getMinPoint();
        if (scopeAmount.compareTo(minPoint) < 0) {
            throw new CouponNotUsableException(BaseCodeEnum.COUPON_MIN_POINT_NOT_REACHED);
        }

        // 抵扣额取券面金额与范围内金额的较小者：券不能被用来减掉范围外的商品，
        // 也不可能把应付金额减成负数
        BigDecimal amount = coupon.getAmount() == null ? BigDecimal.ZERO : coupon.getAmount();

        CouponUsableVo vo = new CouponUsableVo();
        vo.setCouponHistoryId(history.getId());
        vo.setCouponId(coupon.getId());
        vo.setCouponName(coupon.getCouponName());
        vo.setAmount(amount);
        vo.setMinPoint(minPoint);
        vo.setCouponEndTime(coupon.getEndTime());
        vo.setScopeAmount(scopeAmount);
        vo.setDiscountAmount(amount.min(scopeAmount));
        vo.setScopeSkuIds(scope.items().stream().map(CouponCartItemVo::getSkuId).toList());
        return vo;
    }

    /**
     * 券对当前购物车的适用范围判定结果。
     *
     * @param amount 适用范围内的商品金额
     * @param items 命中范围的购物项，顺序与入参一致
     */
    private record Scope(BigDecimal amount, List<CouponCartItemVo> items) {
    }

    /**
     * 一批券的适用范围索引：券模板 ID 到它限定的分类集合与商品集合。
     *
     * <p>先一次性把关联明细读进来按券分组，而不是每张券各查一次 —— 结算页每次加载都会走这条路，
     * 逐张查会变成"会员手里有几张限定券就发几次查询"。
     *
     * @param categoryIds 券模板 ID 到限定分类集合的映射，只含限定分类的券
     * @param spuIds 券模板 ID 到限定商品集合的映射，只含限定商品的券
     */
    private record ScopeIndex(Map<Long, Set<Long>> categoryIds, Map<Long, Set<Long>> spuIds) {

        /**
         * 判断这批券里有没有限定适用范围的。
         *
         * @return {@code true} 表示至少有一张限定分类或指定商品
         */
        boolean hasScoped() {
            return !categoryIds.isEmpty() || !spuIds.isEmpty();
        }

        /**
         * 取一张券限定的分类集合。
         *
         * @param couponId 券模板主键，可以为 {@code null}
         * @return 分类 ID 集合；该券不限定分类时为空集合
         */
        Set<Long> categoriesOf(Long couponId) {
            return categoryIds.getOrDefault(couponId, Set.of());
        }

        /**
         * 取一张券限定的商品集合。
         *
         * @param couponId 券模板主键，可以为 {@code null}
         * @return SPU ID 集合；该券不限定商品时为空集合
         */
        Set<Long> spusOf(Long couponId) {
            return spuIds.getOrDefault(couponId, Set.of());
        }
    }

    /**
     * 算出每张候选券在本次购物车下的适用范围。
     *
     * <p>商品层级归属与关联明细都按批查一次，再逐张套用，所以本方法对单张券与整批券同样适用。
     *
     * @param items 购物项，不能为 {@code null} 或空集合
     * @param coupons 候选券模板，不能为 {@code null}
     * @return 券模板主键到适用范围判定结果的映射，不会返回 {@code null}
     * @throws BaseException 判定适用范围所需的商品服务不可用时抛出
     */
    private Map<Long, Scope> resolveScopes(List<CouponCartItemVo> items, Collection<CouponEntity> coupons) {
        ScopeIndex scopeIndex = loadScopeIndex(coupons);
        Map<Long, SkuScopeVo> skuScopes = resolveSkuScopes(items, scopeIndex);

        Map<Long, Scope> scopes = new HashMap<>();
        for (CouponEntity coupon : coupons) {
            scopes.put(coupon.getId(), resolveScope(coupon, items, skuScopes, scopeIndex));
        }
        return scopes;
    }

    /**
     * 找出单张券适用范围内的购物项并累计它们的金额，也就是门槛比较的基数。
     *
     * <p>全场券就是整单；指定分类与指定商品只留下命中关联明细的那几项 ——
     * 否则"满 100 减 20、仅限图书"会在买 1 本书加 200 元电器时也被放行。
     *
     * @param coupon 券模板，不能为 {@code null}
     * @param items 购物项，不能为 {@code null}
     * @param skuScopes skuId 到商品层级归属的映射，不能为 {@code null}
     * @param scopeIndex 这批券的适用范围索引，不能为 {@code null}
     * @return 范围内的金额与购物项；一项都没命中时金额为 0、列表为空
     */
    private Scope resolveScope(CouponEntity coupon, List<CouponCartItemVo> items,
                               Map<Long, SkuScopeVo> skuScopes, ScopeIndex scopeIndex) {
        Integer useType = coupon.getUseType();
        if (useType == null || useType == SCOPE_ALL) {
            BigDecimal sum = items.stream()
                    .map(CouponCartItemVo::subtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return new Scope(sum, items);
        }

        boolean byCategory = useType == SCOPE_CATEGORY;
        Set<Long> allowed = byCategory
                ? scopeIndex.categoriesOf(coupon.getId())
                : scopeIndex.spusOf(coupon.getId());
        if (allowed.isEmpty()) {
            // 选了指定范围却没有关联明细：这张券谁也匹配不到，与"没命中"同样处理
            return new Scope(BigDecimal.ZERO, List.of());
        }

        BigDecimal sum = BigDecimal.ZERO;
        List<CouponCartItemVo> matched = new ArrayList<>();
        for (CouponCartItemVo item : items) {
            SkuScopeVo itemScope = skuScopes.get(item.getSkuId());
            if (itemScope == null) {
                continue;
            }
            Long key = byCategory ? itemScope.getCatalogId() : itemScope.getSpuId();
            if (key != null && allowed.contains(key)) {
                sum = sum.add(item.subtotal());
                matched.add(item);
            }
        }
        return new Scope(sum, matched);
    }

    /**
     * 判断券是否已经不在可使用期内。
     *
     * <p>两个边界都按"不在期内"处理：起止时刻本身可用会让区间在两端各多出一次判定分歧。
     *
     * @param coupon 券模板，不能为 {@code null}
     * @param now 判定基准时间，不能为 {@code null}
     * @return {@code true} 表示当前不可使用
     */
    private boolean outOfUsePeriod(CouponEntity coupon, Date now) {
        return coupon.getStartTime() != null && now.before(coupon.getStartTime())
                || coupon.getEndTime() != null && now.after(coupon.getEndTime());
    }

    /**
     * 批量读取一批券的适用范围索引。
     *
     * <p>只对真正限定范围的券各发一次查询：一张限定券都没有时不查，也不按券逐张查。
     *
     * @param coupons 候选券模板，不能为 {@code null}
     * @return 适用范围索引，不会返回 {@code null}
     */
    private ScopeIndex loadScopeIndex(Collection<CouponEntity> coupons) {
        List<Long> categoryScoped = new ArrayList<>();
        List<Long> spuScoped = new ArrayList<>();
        for (CouponEntity coupon : coupons) {
            Integer useType = coupon.getUseType();
            if (Integer.valueOf(SCOPE_CATEGORY).equals(useType)) {
                categoryScoped.add(coupon.getId());
            } else if (Integer.valueOf(SCOPE_SPU).equals(useType)) {
                spuScoped.add(coupon.getId());
            }
        }

        Map<Long, Set<Long>> categories = categoryScoped.isEmpty() ? Map.of()
                : couponSpuCategoryRelationService.list(
                                new LambdaQueryWrapper<CouponSpuCategoryRelationEntity>()
                                        .in(CouponSpuCategoryRelationEntity::getCouponId, categoryScoped))
                        .stream()
                        .filter(relation -> relation.getCategoryId() != null)
                        .collect(Collectors.groupingBy(CouponSpuCategoryRelationEntity::getCouponId,
                                Collectors.mapping(CouponSpuCategoryRelationEntity::getCategoryId,
                                        Collectors.toCollection(HashSet::new))));

        Map<Long, Set<Long>> spus = spuScoped.isEmpty() ? Map.of()
                : couponSpuRelationService.list(new LambdaQueryWrapper<CouponSpuRelationEntity>()
                                .in(CouponSpuRelationEntity::getCouponId, spuScoped))
                        .stream()
                        .filter(relation -> relation.getSpuId() != null)
                        .collect(Collectors.groupingBy(CouponSpuRelationEntity::getCouponId,
                                Collectors.mapping(CouponSpuRelationEntity::getSpuId,
                                        Collectors.toCollection(HashSet::new))));

        return new ScopeIndex(categories, spus);
    }

    /**
     * 把购物车里的 skuId 换成所属 SPU 与分类，供范围匹配使用。
     *
     * <p>这批券里一张限定范围的都没有时直接返回空映射，不发起远程调用 —— 会员只持全场券是常见情况。
     *
     * <p>查不到就抛异常而不是退化成"匹配不到"：范围判定不出来时会算出一个偏小的抵扣额，
     * 会员看到的价与下单时校验的价会分叉，比直接报错更难查。
     *
     * @param items 购物项，不能为 {@code null}
     * @param scopeIndex 这批券的适用范围索引，不能为 {@code null}
     * @return skuId 到商品层级归属的映射；不需要范围判定时为空映射
     * @throws BaseException 商品服务不可用或返回失败时抛出
     */
    private Map<Long, SkuScopeVo> resolveSkuScopes(List<CouponCartItemVo> items, ScopeIndex scopeIndex) {
        if (!scopeIndex.hasScoped()) {
            return Map.of();
        }

        List<Long> skuIds = items.stream()
                .map(CouponCartItemVo::getSkuId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (skuIds.isEmpty()) {
            return Map.of();
        }

        R<Map<Long, SkuScopeVo>> response;
        try {
            response = productFeignService.getSkuScopes(skuIds);
        } catch (Exception e) {
            log.error("查询商品层级归属失败，无法判定优惠券适用范围", e);
            throw new BaseException(BaseCodeEnum.COUPON_PRODUCT_UNAVAILABLE);
        }
        if (response == null || response.getCode() != 0 || response.getData() == null) {
            log.error("查询商品层级归属失败，code={}", response == null ? null : response.getCode());
            throw new BaseException(BaseCodeEnum.COUPON_PRODUCT_UNAVAILABLE);
        }
        return response.getData();
    }

    /* ═══════════════════ 内部 ═══════════════════ */

    /**
     * 券不满足使用条件。
     *
     * <p>只有"能不能用"的判定会抛它，其余异常照常向上传播：列券路径逐张 catch 它来跳过不可用的券，
     * 算价路径不 catch，由全局异常处理器转成带原因的错误响应。
     */
    private static class CouponNotUsableException extends BaseException {

        /**
         * 用不可用的原因构造异常。
         *
         * @param reason 不可用的原因，不能为 {@code null}
         */
        CouponNotUsableException(BaseCodeEnum reason) {
            super(reason);
        }
    }

    /**
     * 校验购物项能参与匹配。
     *
     * <p>本类型作为裸 {@code List} 的请求体传入，Spring 的 {@code @RequestBody} 校验拿不到容器元素上的
     * 约束，所以在这里显式判。
     *
     * @param items 购物项，可以为 {@code null}
     * @throws BaseException 购物项为空，或缺少 skuId、单价、数量时抛出
     */
    private void assertItemsValid(List<CouponCartItemVo> items) {
        if (items == null || items.isEmpty()) {
            throw new BaseException(BaseCodeEnum.VALID_EXCEPTION, "购物项不能为空");
        }
        for (CouponCartItemVo item : items) {
            if (item == null || item.getSkuId() == null || item.getPrice() == null
                    || item.getCount() == null || item.getCount() < 1) {
                throw new BaseException(BaseCodeEnum.VALID_EXCEPTION, "购物项缺少 skuId、单价或数量");
            }
        }
    }

    /**
     * 按订单号与使用状态找一条领取记录。
     *
     * @param orderSn 订单号，不能为 {@code null}
     * @param useType 使用状态，不能为 {@code null}
     * @return 命中的领取记录；找不到时返回 {@code null}
     */
    private CouponHistoryEntity findOneByOrderSn(String orderSn, int useType) {
        // 一张订单只占一张券，但 order_sn 上没有唯一索引，取不到就按没有处理而不是抛异常
        return couponHistoryService.getOne(new LambdaQueryWrapper<CouponHistoryEntity>()
                .eq(CouponHistoryEntity::getOrderSn, orderSn)
                .eq(CouponHistoryEntity::getUseType, useType), false);
    }

    /**
     * 为领取记录回填券模板上的展示字段与使用状态文案。
     *
     * @param records 领取记录列表，不能为 {@code null}，可以为空
     */
    private void fillCouponInfo(List<CouponHistoryEntity> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        Map<Long, CouponEntity> coupons = loadCoupons(couponIdsOf(records));
        for (CouponHistoryEntity record : records) {
            record.setUseTypeText(CouponUseStatusEnum.textOf(record.getUseType()));
            CouponEntity coupon = coupons.get(record.getCouponId());
            if (coupon == null) {
                continue;
            }
            record.setCouponName(coupon.getCouponName());
            record.setAmount(coupon.getAmount());
            record.setMinPoint(coupon.getMinPoint());
            record.setCouponStartTime(coupon.getStartTime());
            record.setCouponEndTime(coupon.getEndTime());
        }
    }

    /**
     * 提取一批领取记录指向的券模板主键。
     *
     * @param records 领取记录列表，不能为 {@code null}
     * @return 去重后的券模板主键列表
     */
    private List<Long> couponIdsOf(List<CouponHistoryEntity> records) {
        return records.stream()
                .map(CouponHistoryEntity::getCouponId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    /**
     * 按主键批量查券模板。
     *
     * @param couponIds 券模板主键列表，不能为 {@code null}，可以为空集合
     * @return 券模板主键到模板的映射；空入参时为空映射，不返回 {@code null}
     */
    private Map<Long, CouponEntity> loadCoupons(List<Long> couponIds) {
        if (couponIds.isEmpty()) {
            return Map.of();
        }
        return couponDao.selectByIds(couponIds).stream()
                .collect(Collectors.toMap(CouponEntity::getId, coupon -> coupon, (first, second) -> first));
    }

}

package coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import common.to.ProductReferenceBlockerTo;
import coupon.entity.CouponEntity;
import coupon.entity.CouponSpuRelationEntity;
import coupon.entity.SeckillSessionEntity;
import coupon.entity.SeckillSkuRelationEntity;
import coupon.service.CouponService;
import coupon.service.CouponSpuRelationService;
import coupon.service.ProductReferenceService;
import coupon.service.SeckillSessionService;
import coupon.service.SeckillSkuRelationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * {@link ProductReferenceService} 的默认实现，读四张表：{@code sms_coupon_spu_relation} 与
 * {@code sms_coupon}、{@code sms_seckill_sku_relation} 与 {@code sms_seckill_session}。
 *
 * <p>判定是否有效不需要 JOIN：先按 spuId / skuId 取出关联行，再按 couponId / 场次 ID 批量取父行，
 * 在内存里按 {@code endTime} 分类。关联行最多就是"这个商品被几张券纳入过适用范围、挂在几场秒杀上"，
 * 量级很小；这样也把"有效 / 失效"的判据收在一处，守卫与清理不会分叉。
 *
 * <p>无状态，线程安全。
 */
@Slf4j
@Service
public class ProductReferenceServiceImpl implements ProductReferenceService {

    private final CouponSpuRelationService couponSpuRelationService;

    private final CouponService couponService;

    private final SeckillSkuRelationService seckillSkuRelationService;

    private final SeckillSessionService seckillSessionService;

    /**
     * 注入四张表各自的服务。
     *
     * @param couponSpuRelationService 券与商品的关联服务，按 spuId 查
     * @param couponService 券模板服务，取券名与使用期
     * @param seckillSkuRelationService 秒杀商品关联服务，按 skuId 查
     * @param seckillSessionService 秒杀场次服务，取场次名与结束时间
     */
    public ProductReferenceServiceImpl(CouponSpuRelationService couponSpuRelationService,
                                       CouponService couponService,
                                       SeckillSkuRelationService seckillSkuRelationService,
                                       SeckillSessionService seckillSessionService) {
        this.couponSpuRelationService = couponSpuRelationService;
        this.couponService = couponService;
        this.seckillSkuRelationService = seckillSkuRelationService;
        this.seckillSessionService = seckillSessionService;
    }

    /** {@inheritDoc} */
    @Override
    public ProductReferenceBlockerTo listActiveBlockers(List<Long> spuIds, List<Long> skuIds) {
        Date now = new Date();
        ProductReferenceBlockerTo blockers = new ProductReferenceBlockerTo();
        blockers.setCouponBlockers(activeCouponBlockers(spuIds, now));
        blockers.setSeckillBlockers(activeSeckillBlockers(skuIds, now));
        return blockers;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public int deleteDeadReferences(List<Long> spuIds, List<Long> skuIds) {
        Date now = new Date();
        int removed = deleteDeadCouponRelations(spuIds, now) + deleteDeadSeckillRelations(skuIds, now);
        if (removed > 0) {
            log.info("清理商品失效引用完成：删除券范围关联与秒杀关联共 {} 行，spuIds={}，skuIds={}",
                    removed, spuIds, skuIds);
        }
        return removed;
    }

    /* ═══════════════════ 券的定向范围 ═══════════════════ */

    /**
     * 收集仍然有效的券对这个商品的引用。
     *
     * @param spuIds SPU ID 列表，允许为空
     * @param now 判定基准时间
     * @return 阻塞清单，没有时为空列表
     */
    private List<ProductReferenceBlockerTo.CouponBlocker> activeCouponBlockers(List<Long> spuIds, Date now) {
        List<CouponSpuRelationEntity> relations = couponRelationsOf(spuIds);
        if (relations.isEmpty()) {
            return List.of();
        }

        Map<Long, CouponEntity> coupons = couponsOf(relations);
        List<ProductReferenceBlockerTo.CouponBlocker> blockers = new ArrayList<>();
        for (CouponSpuRelationEntity relation : relations) {
            CouponEntity coupon = coupons.get(relation.getCouponId());
            if (!isCouponActive(coupon, now)) {
                continue;
            }
            ProductReferenceBlockerTo.CouponBlocker blocker = new ProductReferenceBlockerTo.CouponBlocker();
            blocker.setSpuId(relation.getSpuId());
            blocker.setCouponId(relation.getCouponId());
            blocker.setCouponName(coupon.getCouponName());
            blocker.setEndTime(coupon.getEndTime());
            blockers.add(blocker);
        }
        return blockers;
    }

    /**
     * 删掉指向这些 SPU 的、已经过期的券范围行。
     *
     * @param spuIds SPU ID 列表，允许为空
     * @param now 判定基准时间
     * @return 删除行数
     */
    private int deleteDeadCouponRelations(List<Long> spuIds, Date now) {
        List<CouponSpuRelationEntity> relations = couponRelationsOf(spuIds);
        if (relations.isEmpty()) {
            return 0;
        }

        Map<Long, CouponEntity> coupons = couponsOf(relations);
        List<Long> deadIds = relations.stream()
                .filter(relation -> !isCouponActive(coupons.get(relation.getCouponId()), now))
                .map(CouponSpuRelationEntity::getId)
                .toList();
        if (deadIds.isEmpty()) {
            return 0;
        }
        couponSpuRelationService.removeByIds(deadIds);
        return deadIds.size();
    }

    /**
     * 按 spuId 取券范围关联行。
     *
     * @param spuIds SPU ID 列表，允许为 {@code null}
     * @return 关联行；入参没有有效值时为空列表
     */
    private List<CouponSpuRelationEntity> couponRelationsOf(List<Long> spuIds) {
        List<Long> ids = usableIds(spuIds);
        if (ids.isEmpty()) {
            return List.of();
        }
        return couponSpuRelationService.list(new LambdaQueryWrapper<CouponSpuRelationEntity>()
                .in(CouponSpuRelationEntity::getSpuId, ids));
    }

    /**
     * 批量取这些关联行对应的券模板。
     *
     * @param relations 券范围关联行，不能为 {@code null}
     * @return couponId 到券模板的映射；券已经不存在时没有对应项
     */
    private Map<Long, CouponEntity> couponsOf(List<CouponSpuRelationEntity> relations) {
        List<Long> couponIds = relations.stream()
                .map(CouponSpuRelationEntity::getCouponId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (couponIds.isEmpty()) {
            return Map.of();
        }
        return couponService.listByIds(couponIds).stream()
                .collect(Collectors.toMap(CouponEntity::getId, Function.identity(), (first, second) -> first));
    }

    /**
     * 券是否还在使用期内。
     *
     * <p>还没开始与正在进行都算有效 —— 还没开始的券是"以后要用"，它的范围行删了同样会改变它的适用范围。
     *
     * @param coupon 券模板，券已不存在时为 {@code null}
     * @param now 判定基准时间
     * @return {@code true} 表示这张券仍然有效
     */
    private static boolean isCouponActive(CouponEntity coupon, Date now) {
        return coupon != null && (coupon.getEndTime() == null || !now.after(coupon.getEndTime()));
    }

    /* ═══════════════════ 秒杀关联 ═══════════════════ */

    /**
     * 收集没有结束的秒杀场次对这个商品的引用。
     *
     * @param skuIds SKU ID 列表，允许为空
     * @param now 判定基准时间
     * @return 阻塞清单，没有时为空列表
     */
    private List<ProductReferenceBlockerTo.SeckillBlocker> activeSeckillBlockers(List<Long> skuIds, Date now) {
        List<SeckillSkuRelationEntity> relations = seckillRelationsOf(skuIds);
        if (relations.isEmpty()) {
            return List.of();
        }

        Map<Long, SeckillSessionEntity> sessions = sessionsOf(relations);
        List<ProductReferenceBlockerTo.SeckillBlocker> blockers = new ArrayList<>();
        for (SeckillSkuRelationEntity relation : relations) {
            SeckillSessionEntity session = sessions.get(relation.getPromotionSessionId());
            if (!isSessionActive(session, now)) {
                continue;
            }
            ProductReferenceBlockerTo.SeckillBlocker blocker = new ProductReferenceBlockerTo.SeckillBlocker();
            blocker.setSkuId(relation.getSkuId());
            blocker.setSessionId(relation.getPromotionSessionId());
            blocker.setSessionName(session.getName());
            blocker.setEndTime(session.getEndTime());
            blockers.add(blocker);
        }
        return blockers;
    }

    /**
     * 删掉指向这些 SKU 的、已结束或场次已经不存在的秒杀关联行。
     *
     * <p>这里只删库里的关联行，<b>不去通知 seckill 清缓存</b>：已结束场次的缓存条目不会被前台展示
     * （{@code /seckill/front/current} 按当前时间筛场次），清它没有收益；而跟着守卫走的那条路径
     * （后台删关联行）本来就会经 {@code offlineSkus} 把缓存摘掉。
     *
     * @param skuIds SKU ID 列表，允许为空
     * @param now 判定基准时间
     * @return 删除行数
     */
    private int deleteDeadSeckillRelations(List<Long> skuIds, Date now) {
        List<SeckillSkuRelationEntity> relations = seckillRelationsOf(skuIds);
        if (relations.isEmpty()) {
            return 0;
        }

        Map<Long, SeckillSessionEntity> sessions = sessionsOf(relations);
        List<Long> deadIds = relations.stream()
                .filter(relation -> !isSessionActive(sessions.get(relation.getPromotionSessionId()), now))
                .map(SeckillSkuRelationEntity::getId)
                .toList();
        if (deadIds.isEmpty()) {
            return 0;
        }
        seckillSkuRelationService.removeByIds(deadIds);
        return deadIds.size();
    }

    /**
     * 按 skuId 取秒杀关联行。
     *
     * @param skuIds SKU ID 列表，允许为 {@code null}
     * @return 关联行；入参没有有效值时为空列表
     */
    private List<SeckillSkuRelationEntity> seckillRelationsOf(List<Long> skuIds) {
        List<Long> ids = usableIds(skuIds);
        if (ids.isEmpty()) {
            return List.of();
        }
        return seckillSkuRelationService.list(new LambdaQueryWrapper<SeckillSkuRelationEntity>()
                .in(SeckillSkuRelationEntity::getSkuId, ids));
    }

    /**
     * 批量取这些关联行对应的场次。
     *
     * @param relations 秒杀关联行，不能为 {@code null}
     * @return 场次 ID 到场次的映射；场次已经被删掉时没有对应项
     */
    private Map<Long, SeckillSessionEntity> sessionsOf(List<SeckillSkuRelationEntity> relations) {
        List<Long> sessionIds = relations.stream()
                .map(SeckillSkuRelationEntity::getPromotionSessionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (sessionIds.isEmpty()) {
            return Map.of();
        }
        return seckillSessionService.listByIds(sessionIds).stream()
                .collect(Collectors.toMap(SeckillSessionEntity::getId, Function.identity(), (first, second) -> first));
    }

    /**
     * 场次是否还没结束。
     *
     * <p>以 {@code end_time} 为唯一依据。场次表另有 {@code status} 列，但"停用"的场次可能被重新启用，
     * 按它判定会让商品删掉之后那场戏再也演不了，所以不算失效；场次行已经不存在（孤儿关联行）算失效。
     *
     * @param session 场次，已经被删掉时为 {@code null}
     * @param now 判定基准时间
     * @return {@code true} 表示这场还没结束
     */
    private static boolean isSessionActive(SeckillSessionEntity session, Date now) {
        return session != null && (session.getEndTime() == null || !now.after(session.getEndTime()));
    }

    /* ═══════════════════ 公共 ═══════════════════ */

    /**
     * 去掉 {@code null} 并去重。
     *
     * @param ids ID 列表，允许为 {@code null}
     * @return 可用的 ID 列表；入参为 {@code null} 时为空列表
     */
    private static List<Long> usableIds(List<Long> ids) {
        return ids == null ? List.of() : ids.stream().filter(Objects::nonNull).distinct().toList();
    }
}

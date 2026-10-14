package coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import common.utils.R;
import coupon.entity.CouponEntity;
import coupon.entity.CouponHistoryEntity;
import coupon.feign.MemberFeignService;
import coupon.service.BirthdayGrantService;
import coupon.service.CouponHistoryService;
import coupon.service.CouponService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 生日赠券实现：取当天的寿星与启用中的会员赠券，逐张交给既有的发券方法。
 *
 * <p>无状态、线程安全：依赖都由构造器注入，不持有连接与线程。
 */
@Slf4j
@Service("birthdayGrantService")
public class BirthdayGrantServiceImpl implements BirthdayGrantService {

    /** 券类型：会员赠券。生日券取这一档，等级差异由券自己的 {@code member_level} 表达。 */
    private static final int COUPON_TYPE_MEMBER_GIFT = 1;

    /** 领取方式：后台赠送。取值与 {@code CouponServiceImpl} 一致，生日赠券属于赠予而不是会员主动领取。 */
    private static final int GET_TYPE_GRANT = 0;

    /** 券与领取记录，发券与判重都走它们既有的实现。 */
    private final CouponService couponService;
    private final CouponHistoryService couponHistoryService;
    /** 会员服务，只用来取"今天过生日的是谁"。 */
    private final MemberFeignService memberFeignService;

    /**
     * 构造服务，依赖由容器注入，创建后可直接使用。
     *
     * @param couponService 券服务，发券与查生日券都用它
     * @param couponHistoryService 领取记录服务，只用于按年判重
     * @param memberFeignService 会员服务，只用于取当天生日的会员 id
     */
    public BirthdayGrantServiceImpl(CouponService couponService,
                                    CouponHistoryService couponHistoryService,
                                    MemberFeignService memberFeignService) {
        this.couponService = couponService;
        this.couponHistoryService = couponHistoryService;
        this.memberFeignService = memberFeignService;
    }

    /** {@inheritDoc} */
    @Override
    public int grantToday() {
        List<Long> birthdayMemberIds = fetchBirthdayMemberIds();
        if (birthdayMemberIds.isEmpty()) {
            return 0;
        }

        List<CouponEntity> coupons = couponService.list(new LambdaQueryWrapper<CouponEntity>()
                .eq(CouponEntity::getCouponType, COUPON_TYPE_MEMBER_GIFT)
                .eq(CouponEntity::getPublish, 1));
        if (coupons.isEmpty()) {
            return 0;
        }

        int granted = 0;
        for (CouponEntity coupon : coupons) {
            List<Long> targets = excludeGrantedThisYear(coupon.getId(), birthdayMemberIds);
            if (targets.isEmpty()) {
                continue;
            }
            try {
                // 必须走 couponService 这个代理，不能在本类里直接调 grant：事务注解靠代理生效，
                // 同类自调用拿不到事务，"占发行量"与"写领取记录"就不再是一个原子单元
                granted += couponService.grant(coupon.getId(), targets);
            } catch (Exception e) {
                // 单张券发不出去（不在领取窗口、余量为 0、等级表取不到）不该拖住其余券
                log.warn("生日赠券失败，跳过该券：couponId={}, 原因={}", coupon.getId(), e.getMessage());
            }
        }
        return granted;
    }

    /**
     * 取今天生日的会员 id。
     *
     * @return 会员 id 列表；会员服务不可用时为空列表（本轮不发，等下一轮）
     */
    private List<Long> fetchBirthdayMemberIds() {
        R<List<Long>> response;
        try {
            response = memberFeignService.birthdayMemberIds();
        } catch (Exception e) {
            log.warn("查询今天生日的会员失败，本轮不发：{}", e.getMessage());
            return List.of();
        }

        if (response == null || response.getCode() != 0 || response.getData() == null) {
            log.warn("查询今天生日的会员失败，本轮不发：{}", response == null ? "响应为空" : response.getMsg());
            return List.of();
        }
        return response.getData();
    }

    /**
     * 从这批会员里去掉今年已经收到过这张券的人。
     *
     * <p>判重认的是"今年经后台赠送拿到过这张券"：{@code get_type} 区分不了后台手动赠券与生日赠券，
     * 所以后台手动发过同一张券的会员，今年不会再收到生日券。
     *
     * <p>年份比较放在 SQL 里：应用算年份会受时区影响，跨年那几个小时会判错。
     *
     * @param couponId 券 id，不能为 {@code null}
     * @param memberIds 候选会员 id，不能为 {@code null} 或空
     * @return 今年还没收到过这张券的会员 id；全都有过时为空列表
     */
    private List<Long> excludeGrantedThisYear(Long couponId, List<Long> memberIds) {
        List<CouponHistoryEntity> granted = couponHistoryService.list(new LambdaQueryWrapper<CouponHistoryEntity>()
                .select(CouponHistoryEntity::getMemberId)
                .eq(CouponHistoryEntity::getCouponId, couponId)
                .eq(CouponHistoryEntity::getGetType, GET_TYPE_GRANT)
                .in(CouponHistoryEntity::getMemberId, memberIds)
                .apply("YEAR(create_time) = YEAR(NOW())"));
        if (granted.isEmpty()) {
            return memberIds;
        }

        Set<Long> grantedMemberIds = granted.stream()
                .map(CouponHistoryEntity::getMemberId)
                .collect(Collectors.toSet());
        return memberIds.stream().filter(memberId -> !grantedMemberIds.contains(memberId)).toList();
    }

}

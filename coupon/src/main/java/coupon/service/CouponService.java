package coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.exception.BaseException;
import common.query.KeyPageQuery;
import common.vo.PageVO;
import coupon.entity.CouponEntity;

import java.util.List;

/**
 * 优惠券模板的管理端服务：分页查询、新增、修改、删除、发布与停发，以及后台定向发券。
 *
 * <p>继承自 {@link IService} 的通用增删改查不承载业务规则，管理端一律走本接口声明的方法 ——
 * 直接调 {@code save} / {@code updateById} / {@code removeByIds} 会绕过关键字段锁定与删除拦截。
 */
public interface CouponService extends IService<CouponEntity> {

    /**
     * 分页查询优惠券，{@code key} 同时模糊匹配券名与优惠券 ID。
     *
     * <p>实现方必须为每行回填 {@code statusText} 与 {@code allowedActions}，且同一次查询内所有行
     * 使用同一个判定时间，否则跨行的状态会出现先后不一致。
     *
     * @param query 分页参数与关键字，不能为 {@code null}；{@code key} 可以为 {@code null} 或空白
     * @return 分页结果，无数据时 {@code rows} 为空列表、{@code total} 为 0，不返回 {@code null}
     */
    PageVO<CouponEntity> queryPage(KeyPageQuery query);

    /**
     * 查询券详情，同时带出适用范围关联明细，供管理端编辑时回显。
     *
     * @param id 优惠券主键，不能为 {@code null}
     * @return 券详情；{@code id} 不存在时返回 {@code null}
     */
    CouponEntity getDetail(Long id);

    /**
     * 新增优惠券，并写入适用范围关联。
     *
     * <p>实现方必须保证：券与关联明细在同一事务内落库；{@code publish}、{@code receiveCount}、
     * {@code useCount} 由服务端决定并忽略入参 —— 新建的券一律是未发布的草稿。
     *
     * @param coupon 券内容，不能为 {@code null}；{@code id} 被忽略
     * @throws BaseException 领取时间区间不合法，或选了指定范围却没有关联明细时抛出
     */
    void saveCoupon(CouponEntity coupon);

    /**
     * 修改优惠券，并全量替换适用范围关联。
     *
     * <p>实现方必须保证：已有会员领取（{@code receiveCount > 0}）时拒绝修改面额、门槛、适用范围、
     * 两个时间区间、每人限领与发行总量 —— 领取记录只存券面 ID，改模板会静默改掉存量券；
     * {@code publish}、{@code receiveCount}、{@code useCount} 忽略入参。
     *
     * @param coupon 券内容，不能为 {@code null}，且 {@code id} 必填
     * @throws BaseException 券不存在、关键字段已锁定，或时间区间与适用范围不合法时抛出
     */
    void updateCoupon(CouponEntity coupon);

    /**
     * 按主键批量删除优惠券，并级联删除适用范围关联。
     *
     * <p>实现方必须保证：任一张券已有领取记录时整批拒绝，不做部分删除 ——
     * 领取记录里的 {@code coupon_id} 一旦指向不存在的券就成了孤儿数据。
     *
     * @param ids 待删除的优惠券主键，不能为 {@code null} 或空集合
     * @throws BaseException 有券已被会员领取时抛出
     */
    void deleteCoupons(List<Long> ids);

    /**
     * 发布优惠券，使其可被领取。
     *
     * <p>实现方必须保证：校验面额与发行总量大于 0、两个时间区间完整且有效期未过、
     * 指定范围时关联明细非空 —— 不满足这些条件的券发布出去也没人领得走。
     *
     * @param id 优惠券主键，不能为 {@code null}
     * @throws BaseException 券不存在，或不满足发布条件时抛出
     */
    void publish(Long id);

    /**
     * 停发优惠券，关闭领取入口。已领取的券不受影响。
     *
     * @param id 优惠券主键，不能为 {@code null}
     * @throws BaseException 券不存在时抛出
     */
    void revoke(Long id);

    /**
     * 后台定向发券给指定会员，写入的领取记录领取方式为后台赠送。
     *
     * <p>实现方必须保证：与用户主动领取走同一套闸门（券已发布且在领取窗口内、还有余量、
     * 目标会员未超出每人限领、会员等级匹配），不因发起方是后台而放行；逐个会员独立判定，
     * 某个会员不满足只跳过该会员，不回滚已发出的部分。
     *
     * @param couponId 优惠券主键，不能为 {@code null}
     * @param memberIds 目标会员主键列表，不能为 {@code null} 或空集合；元素为 {@code null} 时跳过
     * @return 实际发出成功的张数，可能为 0
     * @throws BaseException 券不存在、当前不可领取，或券限定了会员等级时抛出
     */
    int grant(Long couponId, List<Long> memberIds);
}

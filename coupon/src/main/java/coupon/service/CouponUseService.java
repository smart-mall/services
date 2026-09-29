package coupon.service;

import common.exception.BaseException;
import common.to.CouponCartItemVo;
import common.to.CouponUsableVo;
import common.vo.PageVO;
import coupon.entity.CouponHistoryEntity;
import coupon.vo.CouponHistoryPageQuery;

import java.util.List;

/**
 * 已领到手的券的使用链路：查我的券、按购物车筛可用券、下单占用、付款核销、取消退回、过期清理。
 *
 * <p>与 {@link CouponService} 的分工是"发"和"用"：那边管券模板与把券发到会员手里，
 * 这边只动 {@code sms_coupon_history} 的状态，不改券模板的发行量与发布状态。
 *
 * <p>三个状态流转方法（{@code lock} / {@code use} / {@code unlock}）同时也是 order 经 Feign 调用的入口，
 * 它们不经网关，身份由调用方显式传参，不做登录校验。
 *
 * <p>{@code lock} 与 {@code unlock} 成对出现，且都按订单号定位：一张券占用时写入订单号，
 * 核销与退回再按订单号找回来，所以调用方不必自己记住领取记录的主键。
 *
 * <p>实现必须无状态、线程安全：同一实例会同时被会员端的请求线程与 order 的服务间调用线程访问。
 */
public interface CouponUseService {

    /**
     * 分页查询当前会员的券，可按使用状态收窄。
     *
     * <p>实现方必须为每行回填券模板上的展示字段（券名、面额、门槛、有效期）与使用状态文案 ——
     * 领取记录只存 {@code couponId}，不回填的话会员端只能看到一串 ID。
     *
     * @param memberId 会员主键，不能为 {@code null}
     * @param query 分页与筛选条件，不能为 {@code null}；{@code useType} 可以为 {@code null}，
     *              为 {@code null} 时不筛选状态；入参里的 {@code memberId} 被忽略并由本参数覆盖，
     *              会员只能看到自己的券
     * @return 分页结果，无数据时 {@code rows} 为空列表、{@code total} 为 0，不返回 {@code null}
     */
    PageVO<CouponHistoryEntity> queryMyCoupons(Long memberId, CouponHistoryPageQuery query);

    /**
     * 列出当前会员在给定购物车下可用的全部券，每张带出本单能减多少。
     *
     * <p>按抵扣金额从大到小排，金额相同时有效期近的排前面 —— 会员端默认选第一张，
     * 排序本身就是"默认用最划算的那张"这个产品规则。
     *
     * <p>由 order 的结算接口调用后并入结算页数据，没有对应的会员端直连入口：
     * 会员端要券列表就重新拉一次结算数据，再开一个入口会让两边各算一份抵扣额。
     *
     * @param memberId 会员主键，不能为 {@code null}
     * @param items 购物项，不能为 {@code null} 或空集合，元素不能缺少 skuId、单价或数量
     * @return 可用券列表，一张都没有时为空列表，不返回 {@code null}
     * @throws BaseException 购物项不合法，或判定适用范围所需的商品服务不可用时抛出
     */
    List<CouponUsableVo> listUsable(Long memberId, List<CouponCartItemVo> items);

    /**
     * 判定指定的一张券在给定购物车下能不能用、能减多少。
     *
     * <p>下单算价与提交校验共用本方法，保证"结算页看到的抵扣额"与"落库的抵扣额"是同一个数。
     *
     * @param memberId 会员主键，不能为 {@code null}
     * @param items 购物项，不能为 {@code null} 或空集合，元素不能缺少 skuId、单价或数量
     * @param couponHistoryId 指定的领取记录主键，不能为 {@code null}
     * @return 该券的抵扣结果，不会返回 {@code null}
     * @throws BaseException 券不存在或不属于该会员、不在可使用期内、购物车里没有适用商品、
     *                       未达到门槛、购物项不合法，或判定适用范围所需的商品服务不可用时抛出
     */
    CouponUsableVo computeDiscount(Long memberId, List<CouponCartItemVo> items, Long couponHistoryId);

    /**
     * 下单占用：把券从"未使用"置为"占用中"，并记下占用它的订单。
     *
     * <p>实现方必须保证：状态判定与写入是同一条带状态条件的 UPDATE，两个并发订单不可能同时占到同一张券；
     * 占用成功之后，这张券在核销或退回之前不参与任何可用券匹配。
     *
     * <p>调用方必须先经 {@link #computeDiscount} 判定过可用性再调用本方法：本方法只认状态，
     * 不重新比对购物车内容与门槛。
     *
     * @param couponHistoryId 领取记录主键，不能为 {@code null}
     * @param memberId 持券会员主键，不能为 {@code null}
     * @param orderId 订单主键，不能为 {@code null}
     * @param orderSn 订单号，不能为 {@code null}；核销与退回都靠它找回来
     * @throws BaseException 领取记录不存在或不属于该会员、券已过有效期、
     *                       已被其它订单占用，或当前状态不支持占用时抛出
     */
    void lock(Long couponHistoryId, Long memberId, Long orderId, String orderSn);

    /**
     * 付款核销：把券从"占用中"置为"已使用"，并给券模板的已核销计数加一。
     *
     * <p>实现方必须保证幂等：支付回调会重复投递，重复调用只能有一次真正发生状态跃迁，
     * 计数也只在跃迁的那一次加一；已经核销过的订单再次调用按成功处理，不抛异常。
     *
     * @param orderSn 订单号，不能为 {@code null}
     * @throws BaseException 按订单号找不到任何已占用或已核销的领取记录时抛出
     */
    void use(String orderSn);

    /**
     * 取消或超时关单时退回：把券从"占用中"置回"未使用"，并清掉订单号与订单 ID。
     *
     * <p>实现方必须保证幂等且不抛异常：关单消息会重投，第二次调用找不到占用中的行是正常情况。
     * 券在占用期间已过有效期的，退回时直接置为"已过期" —— 置回未使用会得到一张显示可用、
     * 实际用不掉的券。
     *
     * @param orderSn 订单号，不能为 {@code null}
     */
    void unlock(String orderSn);

    /**
     * 把已过有效期的"未使用"券置为"已过期"，供定时任务调用。
     *
     * <p>只处理"未使用"：占用中的券归订单管，已使用的券不参与使用。
     * 实现方必须能安全重跑，且不能因为一次处理不完就把结果丢掉。
     *
     * @return 本次置为已过期的张数，可能为 0
     */
    int expire();
}

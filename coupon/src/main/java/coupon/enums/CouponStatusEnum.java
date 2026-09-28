package coupon.enums;

import coupon.entity.CouponEntity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 优惠券的派生状态，以及各状态下允许执行的动作。
 *
 * <p>状态不落库，由发布开关、领取窗口与领取进度现算。判定的权威定义在本枚举：
 * 其它位置若出现与之不一致的判定，一律以本枚举为准。
 *
 * <p>无状态、线程安全。
 */
public enum CouponStatusEnum {

    /** 已结束：领取窗口已过，不再接受领取。 */
    ENDED("已结束"),
    /** 草稿：未发布且无人领取，可自由编辑、发布或删除。 */
    DRAFT("草稿"),
    /** 已停发：发布过且已有人领取，当前关闭领取；有领取记录，不可删除。 */
    REVOKED("已停发"),
    /** 未开始：已发布但尚未到领取开始时间。 */
    NOT_STARTED("未开始"),
    /** 已领完：已领张数达到发行总量。 */
    SOLD_OUT("已领完"),
    /** 领取中：已发布、在领取窗口内且还有余量。 */
    RECEIVING("领取中");

    /** 状态文案，直接展示在管理端列表。 */
    private final String msg;

    CouponStatusEnum(String msg) {
        this.msg = msg;
    }

    /**
     * 获取状态文案。
     *
     * @return 状态文案，不会为 {@code null}
     */
    public String getMsg() {
        return msg;
    }

    /**
     * 按发布开关、领取窗口与领取进度现算券的状态。
     *
     * <p>判定有优先级：领取窗口结束优先于发布开关，发布开关优先于领取进度。过期的券无论发布与否
     * 都算已结束，否则运营会看到一张标着"领取中"却谁也领不走的券。
     *
     * @param coupon 优惠券模板；{@code publish}、两个领取时间与 {@code receiveCount} 允许为 {@code null}
     * @param now 判定基准时间，不能为 {@code null}；一次列表查询内的所有行必须传同一个值
     * @return 现算出的状态，不会为 {@code null}
     */
    public static CouponStatusEnum resolve(CouponEntity coupon, Date now) {
        // 窗口结束优先判定：过期的券不该因为发布开关还开着就显示成可领
        if (coupon.getEnableEndTime() != null && now.after(coupon.getEnableEndTime())) {
            return ENDED;
        }
        if (!Integer.valueOf(1).equals(coupon.getPublish())) {
            // 有领取记录说明发过又停了，与从未发布的草稿分开：前者不能删
            return receiveCount(coupon) > 0 ? REVOKED : DRAFT;
        }
        if (coupon.getEnableStartTime() != null && now.before(coupon.getEnableStartTime())) {
            return NOT_STARTED;
        }
        if (receiveCount(coupon) >= publishCount(coupon)) {
            return SOLD_OUT;
        }
        return RECEIVING;
    }

    /**
     * 列出券当前允许执行的动作，与 {@link #resolve} 同源。
     *
     * <p>由服务端下发而不是让前端自行推断，避免按钮显隐与后端的守卫判断分叉。
     *
     * @param coupon 优惠券模板
     * @param now 判定基准时间，不能为 {@code null}
     * @return 动作名列表，取值为 {@code edit}、{@code publish}、{@code revoke}、{@code delete}、
     *         {@code grant}；查看领取记录不受状态限制，不在列表中
     */
    public static List<String> allowedActions(CouponEntity coupon, Date now) {
        CouponStatusEnum status = resolve(coupon, now);
        List<String> actions = new ArrayList<>();

        // 编辑始终可用，关键字段是否可改由表单按 receiveCount 判断
        actions.add("edit");
        if (status == DRAFT || status == REVOKED) {
            actions.add("publish");
        } else if (status != ENDED) {
            actions.add("revoke");
        }
        // 有领取记录就不能删：删了领取记录里的 coupon_id 会变成孤儿
        if (receiveCount(coupon) == 0) {
            actions.add("delete");
        }
        if (status == RECEIVING) {
            actions.add("grant");
        }
        return actions;
    }

    /**
     * 读取已领取张数。
     *
     * @param coupon 优惠券模板
     * @return 已领取张数；字段为空时按 0 处理
     */
    private static int receiveCount(CouponEntity coupon) {
        return coupon.getReceiveCount() == null ? 0 : coupon.getReceiveCount();
    }

    /**
     * 读取发行总量。
     *
     * @param coupon 优惠券模板
     * @return 发行总量；字段为空时按 0 处理，使未填发行量的券直接落到已领完
     */
    private static int publishCount(CouponEntity coupon) {
        return coupon.getPublishCount() == null ? 0 : coupon.getPublishCount();
    }
}

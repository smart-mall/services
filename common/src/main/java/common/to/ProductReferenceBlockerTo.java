package common.to;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 某个商品在营销侧删不掉的原因：它还被哪些仍然有效的营销配置覆盖。
 *
 * <p>跨服务传输：coupon 的判断接口返回它，product 拿它拼给用户看的提示。
 * 所以只回答"卡在哪"，措辞留给调用方 —— 与 {@link SkuDeleteBlockerTo} 同一形状。
 *
 * <p><b>是否仍然有效只看时间</b>：券的 {@code end_time} 早于当前时间算已过使用期、
 * 场次的 {@code end_time} 早于当前时间算已结束，两者都不计入本清单，它们的关联行
 * 由商品删除后的异步清理直接删掉；{@code end_time} 为 {@code null} 表示永久有效，算有效。
 */
@Data
public class ProductReferenceBlockerTo implements Serializable {

    /** 还被券的适用范围覆盖的 SPU；没有时为空列表。 */
    private List<CouponBlocker> couponBlockers;

    /** 还挂在没有结束的秒杀场次上的 SKU；没有时为空列表。 */
    private List<SeckillBlocker> seckillBlockers;

    /** 一张还在使用期内的券把这个 SPU 纳入了适用范围。 */
    @Data
    public static class CouponBlocker implements Serializable {
        /** 被圈住的 SPU ID。 */
        private Long spuId;
        /** 券 ID。 */
        private Long couponId;
        /** 券名，供提示语直接展示。 */
        private String couponName;
        /** 券的使用期结束时间；{@code null} 表示永久有效。 */
        private Date endTime;
    }

    /** 一个没有结束的秒杀场次上还挂着这个 SKU。 */
    @Data
    public static class SeckillBlocker implements Serializable {
        /** 挂在场次上的 SKU ID。 */
        private Long skuId;
        /** 场次 ID。 */
        private Long sessionId;
        /** 场次名，供提示语直接展示。 */
        private String sessionName;
        /** 场次结束时间；{@code null} 表示没有结束时间。 */
        private Date endTime;
    }
}

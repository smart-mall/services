package common.to;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 某个分类在营销侧删不掉的原因：它还被仍然有效的券纳入了适用范围。
 *
 * <p>跨服务传输：coupon 的判断接口返回它，product 拿它拼给用户看的提示。
 * 所以只回答"卡在哪"，措辞留给调用方 —— 与 {@link ProductReferenceBlockerTo} 同一形状。
 *
 * <p><b>是否仍然有效只看时间</b>：券的 {@code end_time} 早于当前时间算已过使用期、不计入本清单，
 * 它的范围行由分类删除后的异步清理直接删掉；{@code end_time} 为 {@code null} 表示永久有效，算有效。
 */
@Data
public class CategoryCouponBlockerTo implements Serializable {

    /** 被券圈进适用范围的分类 ID。 */
    private Long categoryId;

    /** 券 ID。 */
    private Long couponId;

    /** 券名，供提示语直接展示。 */
    private String couponName;

    /** 券的使用期结束时间；{@code null} 表示永久有效。 */
    private Date endTime;
}

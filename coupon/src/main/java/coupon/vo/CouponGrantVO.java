package coupon.vo;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 后台定向发券的入参，对应 {@code POST /coupon/coupon/{id}/grant}：把一张券发给选中的会员。
 *
 * <p>只带会员 ID，不带昵称等展示字段 —— 昵称属于会员资料，由发券侧跨库去取而不是让调用方传。
 */
@Data
public class CouponGrantVO {

    /** 目标会员主键列表，不能为空；列表内的 {@code null} 元素会被跳过。 */
    @NotEmpty(message = "请选择要发券的会员")
    private List<Long> memberIds;
}

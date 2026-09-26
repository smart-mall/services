package cart.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import static common.constant.CartConstant.MAX_ITEM_COUNT;
import static common.constant.CartConstant.MIN_ITEM_COUNT;

/**
 * 修改购物项数量请求体。
 *
 * <p>数量区间与加购一致，由校验注解保证，0 或负数在进入 Service 前就被拦下。</p>
 */
@Data
public class ChangeItemCountVo {

    /** 目标数量，绝对值而非增量，取值区间 {@code [1, 99]}。 */
    @NotNull(message = "不能为空")
    @Min(value = MIN_ITEM_COUNT, message = "不能小于1")
    @Max(value = MAX_ITEM_COUNT, message = "不能大于99")
    private Integer num;

}

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
 * <p>和加购用同一个区间。原实现 {@code GET /countItem?skuId=&num=} 不校验，
 * num=0 或负数都会原样写进 Redis。</p>
 */
@Data
public class ChangeItemCountVo {

    @NotNull(message = "不能为空")
    @Min(value = MIN_ITEM_COUNT, message = "不能小于1")
    @Max(value = MAX_ITEM_COUNT, message = "不能大于99")
    private Integer num;

}

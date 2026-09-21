package cart.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import static common.constant.CartConstant.MAX_ITEM_COUNT;
import static common.constant.CartConstant.MIN_ITEM_COUNT;

/**
 * 加入购物车请求体。
 *
 * <p>数量上下限写在校验注解上而不是 Service 里手写 if：这样越界走的是
 * {@code MethodArgumentNotValidException} → 统一返回 {@code code:10001 + errors{num:...}}，
 * 和项目里其它接口的入参错误格式一致，前端只写一个分支。</p>
 *
 * <p>原实现是 {@code GET /addCartItem?skuId=&num=}，num 完全不校验，
 * 传负数能把购物车里的数量直接减成负数。</p>
 */
@Data
public class AddCartItemVo {

    @NotNull(message = "不能为空")
    private Long skuId;

    @NotNull(message = "不能为空")
    @Min(value = MIN_ITEM_COUNT, message = "不能小于1")
    @Max(value = MAX_ITEM_COUNT, message = "不能大于99")
    private Integer num;

}

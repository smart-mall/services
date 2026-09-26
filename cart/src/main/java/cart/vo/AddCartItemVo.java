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
 * <p>数量上下限写在校验注解上而不是在 Service 里手写判断：越界由
 * {@code MethodArgumentNotValidException} 统一转成 {@code code:10001} 加 {@code data.errors}，
 * 与其它接口的入参错误格式一致，前端只需一个分支。</p>
 */
@Data
public class AddCartItemVo {

    /** 商品 SKU 标识。 */
    @NotNull(message = "不能为空")
    private Long skuId;

    /** 加购数量增量，取值区间 {@code [1, 99]}。 */
    @NotNull(message = "不能为空")
    @Min(value = MIN_ITEM_COUNT, message = "不能小于1")
    @Max(value = MAX_ITEM_COUNT, message = "不能大于99")
    private Integer num;

}

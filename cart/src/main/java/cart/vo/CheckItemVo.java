package cart.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 勾选单个购物项请求体。
 *
 * <p>{@code checked} 是布尔值，只表达勾选与取消勾选两种状态，不用 0/1 之类的数字取值。</p>
 */
@Data
public class CheckItemVo {

    /** 目标勾选状态：{@code true} 勾选，{@code false} 取消勾选。 */
    @NotNull(message = "不能为空")
    private Boolean checked;

}

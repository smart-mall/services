package cart.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 勾选单个购物项请求体。
 *
 * <p>{@code checked} 用 Boolean 而不是原来的 {@code Integer checked}（0/1）：
 * 原来那套是 {@code check == 1 ? true : false}，传 2、传 -1 都静默当 false，
 * 前端一个手误就变成"取消勾选"且没有任何提示。</p>
 */
@Data
public class CheckItemVo {

    @NotNull(message = "不能为空")
    private Boolean checked;

}

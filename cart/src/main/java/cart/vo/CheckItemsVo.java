package cart.vo;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 批量勾选 / 全选反选请求体。
 *
 * <p>{@code skuIds} 必须显式给出，不做"不传就代表全部"的默认：漏传会把整辆车勾上，
 * 用户很难发现。</p>
 */
@Data
public class CheckItemsVo {

    /** 待修改勾选状态的 SKU 标识列表，不能为空。 */
    @NotEmpty(message = "不能为空")
    private List<Long> skuIds;

    /** 目标勾选状态：{@code true} 勾选，{@code false} 取消勾选。 */
    @NotNull(message = "不能为空")
    private Boolean checked;

}

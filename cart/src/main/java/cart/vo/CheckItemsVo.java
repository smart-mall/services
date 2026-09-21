package cart.vo;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 批量勾选 / 全选反选请求体。
 *
 * <p>{@code skuIds} 必须显式给出，不做"不传就代表全部"的默认：
 * 全选是"勾选车里所有商品"，前端本来就知道车里有哪几个 sku，
 * 而服务端一旦允许省略这个字段，一个漏传的请求就会把整辆车勾上，用户很难发现。</p>
 */
@Data
public class CheckItemsVo {

    @NotEmpty(message = "不能为空")
    private List<Long> skuIds;

    @NotNull(message = "不能为空")
    private Boolean checked;

}

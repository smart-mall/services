package common.to;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 删除商品前向营销侧核对引用所需的主键。
 *
 * <p>券的定向范围挂在 SPU 上、秒杀关联挂在 SKU 上，所以两个都带：调用方（product）手里
 * 正好两者都有，不必让 coupon 再回查一次商品归属。
 *
 * <p>跨服务传输：product 经 Feign 直连 coupon 的判断接口时作为请求体。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDeleteCheckTo implements Serializable {

    /** 待删除的 SPU ID 列表，用于查券的定向范围；允许为 {@code null}。 */
    private List<Long> spuIds;

    /** 待删除的 SKU ID 列表，用于查秒杀关联；允许为 {@code null}。 */
    private List<Long> skuIds;
}

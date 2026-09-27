package order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 运费计算结果，面向前端：整单的三个金额与按商品拆分的运费明细。
 *
 * <p>三个金额都由服务端算定，前端只负责显示并原样回传 {@code payAmount}。
 * 前端若自己按「商品总额 + 运费」再算一遍，服务端一改加价规则就会算出两个数，
 * 提交时被判成价格变动。
 */
@Data
public class FareVo {

    /** 商品总额，不含运费。 */
    private BigDecimal totalAmount;

    /** 整单运费。 */
    private BigDecimal freightAmount;

    /** 应付总额 = 商品总额 + 运费。 */
    private BigDecimal payAmount;

    /** 每个商品的运费明细，前端按它逐行展示。 */
    private List<FareItemVo> fareItems;
}

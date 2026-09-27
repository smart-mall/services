package order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** ware 计费接口的出参：整单运费与按商品拆分的明细。 */
@Data
public class WareFareVo {

    /** 整单运费，等于明细之和，由 ware 算定。 */
    private BigDecimal totalFare;

    /** 每个商品的运费明细。 */
    private List<WareFareItemResultVo> items;
}

package ware.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 运费计算结果：整单运费与按商品拆分的明细。
 *
 * <p>{@code totalFare} 是明细之和，由本服务算定；调用方直接用这个数，不要自行汇总，
 * 否则两边的舍入方式一旦不同就会算出两个金额。
 */
@Data
public class FareVo {

    /** 整单运费，单位元，等于 {@code items} 各项之和。 */
    private BigDecimal totalFare;

    /** 每个商品的运费明细。 */
    private List<FareItemResultVo> items;
}

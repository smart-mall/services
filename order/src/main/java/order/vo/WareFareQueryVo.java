package order.vo;

import lombok.Data;

import java.util.List;

/** 向 ware 请求计费的入参：收货地区划编码与要计价的商品清单。 */
@Data
public class WareFareQueryVo {

    /** 收货地的行政区划编码。 */
    private String destNode;

    /** 要计价的商品清单。 */
    private List<WareFareQueryItemVo> items;
}

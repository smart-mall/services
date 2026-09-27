package ware.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/** 运费计算入参：收货地区划编码与该地址要发货的商品清单。 */
@Data
public class FareQueryVo {

    /** 收货地的行政区划编码，本服务按它和发货仓的编码算直线距离。 */
    @NotBlank(message = "缺少收货地区划编码")
    private String destNode;

    /** 要计价的商品清单，不能为空。 */
    @NotEmpty(message = "缺少要计价的商品")
    @Valid
    private List<FareQueryItemVo> items;
}

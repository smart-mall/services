package ware.vo;

import lombok.Data;

import java.math.BigDecimal;


/**
 * 运费计算的出参，对应 {@code GET /ware/wareinfo/fare}：返回收货地址与按该地址算出的运费，
 * 供 order 侧结算页展示、下单时计入订单运费。
 */
@Data
public class FareVo {

    /** 参与计算运费的收货地址，来自 member 服务的地址详情。 */
    private MemberAddressVo address;

    /** 运费金额，单位元；地址手机号不足 10 位无法计算时按 0 返回。 */
    private BigDecimal fare;

}



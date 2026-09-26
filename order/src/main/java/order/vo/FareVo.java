package order.vo;

import lombok.Data;

import java.math.BigDecimal;


/** 运费计算结果：收货地址 + 该地址对应的运费。 */
@Data
public class FareVo {

    /** 参与计算运费的收货地址。 */
    private MemberAddressVo address;

    /** 运费金额，单位元。 */
    private BigDecimal fare;

}

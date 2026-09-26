package order.vo;

import lombok.Data;


/**
 * 收货地址，结算页展示与算运费时由 order 侧只读使用。
 */
@Data
public class MemberAddressVo {

    /** 地址 ID。 */
    private Long id;

    /** 所属会员 ID。 */
    private Long memberId;

    /** 收货人姓名。 */
    private String name;

    /** 收货人手机号。 */
    private String phone;

    /** 邮政编码。 */
    private String postCode;

    /** 省 / 直辖市名称。 */
    private String province;

    /** 城市名称。 */
    private String city;

    /** 区县名称。 */
    private String region;

    /** 详细地址，含街道与门牌号。 */
    private String detailAddress;

    /** 省市区代码。 */
    private String areacode;

    /** 是否默认地址：1 是，0 否。 */
    private Integer defaultStatus;

}

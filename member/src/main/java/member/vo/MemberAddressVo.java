package member.vo;

import lombok.Data;


/** 收货地址的传输对象：新增地址接口的请求体与返回体共用。 */
@Data
public class MemberAddressVo {

    /** 地址 ID。 */
    private Long id;

    /** 所属会员 ID。 */
    private Long memberId;
    /** 收货人姓名。 */
    private String name;
    /** 电话。 */
    private String phone;
    /** 邮政编码。 */
    private String postCode;
    /** 省份/直辖市。 */
    private String province;
    /** 城市。 */
    private String city;
    /** 区。 */
    private String region;
    /** 详细地址(街道)。 */
    private String detailAddress;
    /** 省市区代码。 */
    private String areacode;
    /** 是否默认。 */
    private Integer defaultStatus;

}

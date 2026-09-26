package ware.vo;

import lombok.Data;


/**
 * 收货地址载体：ware 侧算运费时经 {@code MemberFeignService#info} 从 member 服务取回的地址详情。
 */
@Data
public class MemberAddressVo {

    /** 地址 ID，即运费计算的入参 {@code addrId}。 */
    private Long id;
    /**
     * 所属会员 ID。
     */
    private Long memberId;
    /**
     * 收货人姓名。
     */
    private String name;
    /**
     * 收货人电话，运费按其倒数第 10、9 位推导。
     */
    private String phone;
    /**
     * 邮政编码。
     */
    private String postCode;
    /**
     * 省 / 直辖市。
     */
    private String province;
    /**
     * 城市。
     */
    private String city;
    /**
     * 区 / 县。
     */
    private String region;
    /**
     * 详细地址（街道门牌）。
     */
    private String detailAddress;
    /**
     * 省市区行政编码。
     */
    private String areacode;
    /**
     * 是否为默认收货地址 [0-否，1-是]。
     */
    private Integer defaultStatus;

}

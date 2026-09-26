package ware.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;


/**
 * 订单状态查询的出参：ware 侧按订单号经 {@code OrderFeignService#getOrderStatus} 回查订单，
 * 用于判断锁定中的库存该不该释放。
 *
 * <p>字段与 order 侧订单实体对应，ware 只读 {@code status}：订单不存在或状态为已取消(4)时解锁库存。
 */
@Data
public class OrderVo {

    /** 订单 ID。 */
    private Long id;
    /** 所属会员 ID。 */
    private Long memberId;
    /** 订单号，业务主键，ware 侧按它回查订单。 */
    private String orderSn;
    /**
     * 使用的优惠券 ID，未使用优惠券时为 {@code null}。
     */
    private Long couponId;
    /** 下单时间。 */
    private Date createTime;
    /** 下单会员的用户名。 */
    private String memberUsername;
    /** 订单总额，单位元，为各订单项优惠后金额之和，不含运费。 */
    private BigDecimal totalAmount;
    /** 应付总额，单位元，等于订单总额加运费。 */
    private BigDecimal payAmount;
    /** 运费金额，单位元。 */
    private BigDecimal freightAmount;
    /** 促销优化金额，单位元（促销价、满减、阶梯价）。 */
    private BigDecimal promotionAmount;
    /** 积分抵扣金额，单位元。 */
    private BigDecimal integrationAmount;
    /** 优惠券抵扣金额，单位元。 */
    private BigDecimal couponAmount;
    /** 后台调整订单使用的折扣金额，单位元。 */
    private BigDecimal discountAmount;
    /**
     * 支付方式【1->支付宝；2->微信；3->银联； 4->货到付款；】，未支付时为 {@code null}。
     */
    private Integer payType;
    /** 订单来源[0->PC订单；1->app订单]。 */
    private Integer sourceType;
    /**
     * 订单状态：0 待付款、1 已付款、2 已发货、3 已完成、4 已取消、5 售后中、6 售后完成，
     * 取值以 order 侧的订单状态枚举为准。
     */
    private Integer status;
    /** 物流公司(配送方式)。 */
    private String deliveryCompany;
    /** 物流单号。 */
    private String deliverySn;
    /** 自动确认收货的天数，下单时写入 7。 */
    private Integer autoConfirmDay;
    /** 本单可获得的积分。 */
    private Integer integration;
    /** 本单可获得的成长值。 */
    private Integer growth;
    /** 发票类型[0->不开发票；1->电子发票；2->纸质发票]。 */
    private Integer billType;
    /** 发票抬头。 */
    private String billHeader;
    /** 发票内容。 */
    private String billContent;
    /** 收票人电话。 */
    private String billReceiverPhone;
    /** 收票人邮箱。 */
    private String billReceiverEmail;
    /** 收货人姓名。 */
    private String receiverName;
    /** 收货人电话。 */
    private String receiverPhone;
    /** 收货人邮编。 */
    private String receiverPostCode;
    /** 省 / 直辖市。 */
    private String receiverProvince;
    /** 城市。 */
    private String receiverCity;
    /** 区 / 县。 */
    private String receiverRegion;
    /** 详细地址（街道门牌）。 */
    private String receiverDetailAddress;
    /** 订单备注。 */
    private String note;
    /** 确认收货状态[0->未确认；1->已确认]，下单时写入 0。 */
    private Integer confirmStatus;
    /** 删除状态【0->未删除；1->已删除】，下单时写入 0。 */
    private Integer deleteStatus;
    /** 下单时使用的积分。 */
    private Integer useIntegration;
    /** 支付时间，未支付时为 {@code null}。 */
    private Date paymentTime;
    /** 发货时间，未发货时为 {@code null}。 */
    private Date deliveryTime;
    /** 确认收货时间，未确认时为 {@code null}。 */
    private Date receiveTime;
    /** 评价时间，未评价时为 {@code null}。 */
    private Date commentTime;
    /** 修改时间。 */
    private Date modifyTime;

}

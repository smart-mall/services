package order.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 会员端看到的售后申请，是 {@code oms_order_return_apply} 的公开投影。
 *
 * <p><b>为什么不直接回实体：</b>实体上有一个 {@code handleMan}（处理这条申请的管理员用户名），
 * 那是内部人员信息、会员不需要知道；直接回实体还意味着以后给表加任何一列都会自动出现在会员端，
 * 包括加给后台用的列。这与评价对外只给公开投影 VO 是同一处理。
 *
 * <p>留下的字段都是会员自己要知道的：退多少、退到哪一步、被拒的理由是什么、
 * 退货地址与收件人是谁（会员要照着寄回去）。
 */
@Getter
@Setter
public class MemberAfterSaleVo {

    /** 售后申请单主键。 */
    private Long id;

    /** 订单号。 */
    private String orderSn;

    /** 售后类型，取值见 {@link order.enums.AfterSaleTypeEnum}。 */
    private Integer type;

    /** 售后类型文案。 */
    private String typeText;

    /** 申请状态，取值见 {@link order.enums.AfterSaleStatusEnum}。 */
    private Integer status;

    /** 申请状态文案。 */
    private String statusText;

    /** 退款金额：仅退款为整单实付，退货为该订单项的实付金额。 */
    private BigDecimal returnAmount;

    /** 退货的商品 SKU ID；仅退款的单为 {@code null}（不涉及具体商品）。 */
    private Long skuId;

    /** 退货的商品名称；仅退款的单为 {@code null}。 */
    private String skuName;

    /** 退货的商品图片；仅退款的单为 {@code null}。 */
    private String skuImg;

    /** 退货的商品销售属性；仅退款的单为 {@code null}。 */
    private String skuAttrsVals;

    /** 退货数量；仅退款的单为 {@code null}。 */
    private Integer skuCount;

    /** 退货原因的补充描述。 */
    private String description;

    /** 退货原因；仅退款的单为 {@code null}（没有对应的原因字典）。 */
    private String reason;

    /** 凭证图片，逗号隔开；仅退款的单为 {@code null}。 */
    private String descPics;

    /** 申请时间。 */
    private Date createTime;

    /** 处理时间，即后台审核的时间。 */
    private Date handleTime;

    /** 处理备注，拒绝时是驳回理由。 */
    private String handleNote;

    /** 商家的退货地址，审核通过后下发，会员照着寄回；仅退款的单为 {@code null}。 */
    private String companyAddress;

    /** 商家的收货人，审核通过后下发；仅退款的单为 {@code null}。 */
    private String receiveMan;

    /** 商家的收货电话，审核通过后下发；仅退款的单为 {@code null}。 */
    private String receivePhone;

    /** 商家确认收到退货的时间；未收到时为 {@code null}。 */
    private Date receiveTime;

    /** 商家的收货备注。 */
    private String receiveNote;
}

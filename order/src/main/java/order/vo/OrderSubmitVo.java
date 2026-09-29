package order.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/** 提交订单的入参。 */

@Data
public class OrderSubmitVo {

    /** 收货地址 ID。 */
    @NotNull(message = "请选择收货地址")
    private Long addrId;

    /** 支付方式，取值见 {@link order.constant.PayConstant}。 */
    @NotNull(message = "请选择支付方式")
    private Integer payType;

    // 不提交要购买的商品列表：下单时按会员的购物车重新取一遍
    // 发票尚未支持

    /**
     * 使用的优惠券领取记录 ID；不用券时不传。
     *
     * <p>只传这个 ID，不传抵扣额：金额由券服务按购物车重算，与结算页展示的同源，
     * 前端传金额等于把优惠额交给客户端决定。
     */
    private Long couponHistoryId;

    /** 防重令牌。 */
    @NotBlank(message = "缺少防重令牌，请返回结算页重试")
    private String orderToken;

    /** 应付价格。前端回传「商品总额 + 运费 - 优惠券抵扣额」（由 {@code /order/front/jwt/confirm} 给全），提交时和重算结果比对。 */
    @NotNull(message = "缺少应付金额")
    private BigDecimal payPrice;

    /** 订单备注。 */
    private String remarks;

    // 会员 id 不在这里：取网关注入的 X-Member-Claims，不接受前端传
}

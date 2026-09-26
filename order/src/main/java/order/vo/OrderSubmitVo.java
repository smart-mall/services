package order.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 封装订单提交数据的vo
 */

@Data
public class OrderSubmitVo {

    /** 收获地址的id */
    @NotNull(message = "请选择收货地址")
    private Long addrId;

    /** 支付方式 */
    @NotNull(message = "请选择支付方式")
    private Integer payType;
    //无需提交要购买的商品，去购物车再获取一遍
    //优惠、发票

    /** 防重令牌 */
    @NotBlank(message = "缺少防重令牌，请返回结算页重试")
    private String orderToken;

    /** 应付价格。前端回传"商品总额 + 运费"（由 {@code /order/front/jwt/confirm} 给全），提交时和重算结果比对 */
    @NotNull(message = "缺少应付金额")
    private BigDecimal payPrice;

    /** 订单备注 */
    private String remarks;

    /** 会员 id 不在这里：取网关注入的 {@code X-Member-Claims}，不接受前端传 */
}

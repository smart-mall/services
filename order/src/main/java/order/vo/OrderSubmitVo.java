package order.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 封装订单提交数据的vo
 *
 * @author 夏沫止水
 * @createTime 2020-07-04 11:54
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

    /**
     * 应付价格。
     *
     * <p>前端回传的是"商品总额 + 运费"（这两个数由 {@code /order/front/confirm} 给全），
     * 后端在 {@code submitOrder} 里和重新算出来的金额比对，不一致就拒单。</p>
     */
    @NotNull(message = "缺少应付金额")
    private BigDecimal payPrice;

    /** 订单备注 */
    private String remarks;

    /** 会员 id 不在这里：从 {@code LoginUserInterceptor} 的 ThreadLocal 取，不接受前端传 */
}

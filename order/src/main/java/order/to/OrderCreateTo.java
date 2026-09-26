package order.to;

import lombok.Data;
import order.entity.OrderEntity;
import order.entity.OrderItemEntity;

import java.math.BigDecimal;
import java.util.List;


/**
 * 下单过程中组装的订单草稿：订单主表与订单项，供落库与验价使用。
 */
@Data
public class OrderCreateTo {

    /** 订单主表，订单号、收货人与运费已填好。 */
    private OrderEntity order;

    /** 该订单的全部订单项。 */
    private List<OrderItemEntity> orderItems;

    /** 订单计算的应付价格。 */
    private BigDecimal payPrice;

    /** 运费。 */
    private BigDecimal fare;

}

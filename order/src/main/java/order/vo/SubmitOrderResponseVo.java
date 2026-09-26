package order.vo;

import lombok.Data;
import order.entity.OrderEntity;

/**
 * 提交订单的返回。
 *
 * <p>失败不在这里表达：一律抛 {@code BaseException} 走 {@code R.error(code, msg)}。
 * 本对象因此只有成功一种形态，也不再自带 {@code code} 字段——和 {@code R.code} 并存会让前端误判。</p>
 */
@Data
public class SubmitOrderResponseVo {

    /** 落库后的订单，含订单号与应付金额。 */
    private OrderEntity order;

}

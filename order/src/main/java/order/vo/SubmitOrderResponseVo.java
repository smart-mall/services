package order.vo;

import lombok.Data;
import order.entity.OrderEntity;

/**
 * 提交订单的返回。
 *
 * <p>原来带一个 {@code code} 字段，用 1/2/3 表示"令牌失效/价格变动/库存不足"，和 {@code R.code}
 * （0=成功）是**两个命名空间**。控制器套上 R 之后响应里会出现两个 code，前端要先判
 * {@code body.code} 再判 {@code data.code}，极容易把失败当成功。现在失败一律抛
 * {@code BaseException} 走 {@code R.error(code, msg)}，这个字段就没有存在意义了。</p>
 */
@Data
public class SubmitOrderResponseVo {

    private OrderEntity order;

}

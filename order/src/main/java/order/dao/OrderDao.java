package order.dao;

import order.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * {@code oms_order} 表的 MyBatis-Plus Mapper，映射订单主表 {@link OrderEntity}。
 *
 * <p>自定义 SQL 只有两条状态流转：{@link #updateOrderStatus} 服务于支付，{@link #deliverOrder}
 * 服务于发货；其余使用 {@link BaseMapper} 提供的通用增删改查。
 */
@Mapper
public interface OrderDao extends BaseMapper<OrderEntity> {

    /**
     * 按订单号更新订单状态、支付方式与支付时间。
     *
     * <p>SQL 见 {@code mapper/order/OrderDao.xml}：{@code modify_time} 与 {@code payment_time}
     * 总是被置为当前时间，与传入的 {@code code} 无关，因此只适用于支付相关的状态流转。
     *
     * @param orderSn 订单号，业务主键，不能为 {@code null}
     * @param code 目标状态码，取值见 {@link order.enums.OrderStatusEnum}
     * @param payType 支付方式，取值见 {@code PayConstant}
     */
    void updateOrderStatus(String orderSn, Integer code, Integer payType);

    /**
     * 发货：把订单从"已付款"改成"已发货"，并写入物流公司与物流单号。
     *
     * <p>SQL 见 {@code mapper/order/OrderDao.xml}：目标状态与前置状态都固化在语句里，
     * {@code WHERE} 带 {@code status = 1}，判断与写入是同一条 SQL，因此并发点两次发货只有一个会成功。</p>
     *
     * <p><b>刻意不写 {@code payment_time} 也不写 {@code pay_type}</b>，与 {@link #updateOrderStatus}
     * 分开：那条语句无条件把两个支付字段置为当前值，拿来做发货会把发货时间写成付款时间、
     * 并覆盖掉真实的支付方式。</p>
     *
     * @param orderSn 订单号，业务主键，不能为 {@code null}
     * @param deliveryCompany 物流公司（配送方式），不能为 {@code null}
     * @param deliverySn 物流单号，不能为 {@code null}
     * @return 受影响行数，1 表示发货成功，0 表示订单不存在或不是已付款状态
     */
    int deliverOrder(String orderSn, String deliveryCompany, String deliverySn);
}

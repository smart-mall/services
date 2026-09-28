package order.dao;

import order.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * {@code oms_order} 表的 MyBatis-Plus Mapper，映射订单主表 {@link OrderEntity}。
 *
 * <p>除 {@link #updateOrderStatus} 外未声明其它自定义 SQL，其余使用 {@link BaseMapper} 提供的通用增删改查。
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
}

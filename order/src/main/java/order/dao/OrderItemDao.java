package order.dao;

import order.entity.OrderItemEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * {@code oms_order_item} 表的 MyBatis-Plus Mapper，映射订单项 {@link OrderItemEntity}。
 *
 * <p>未声明自定义 SQL，仅使用 {@link BaseMapper} 提供的通用增删改查。
 */
@Mapper
public interface OrderItemDao extends BaseMapper<OrderItemEntity> {
	
}

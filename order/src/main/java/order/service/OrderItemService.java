package order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import order.entity.OrderItemEntity;

import java.util.Map;

import common.query.PageQuery;
/**
 * 订单项信息
 */
public interface OrderItemService extends IService<OrderItemEntity> {

    PageVO<OrderItemEntity> queryPage(PageQuery query);
}


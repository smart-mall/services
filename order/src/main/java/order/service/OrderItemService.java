package order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import order.entity.OrderItemEntity;

import java.util.Map;

/**
 * 订单项信息
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 11:22:13
 */
public interface OrderItemService extends IService<OrderItemEntity> {

    PageVO<OrderItemEntity> queryPage(Map<String, Object> params);
}


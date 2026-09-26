package order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import order.entity.OrderOperateHistoryEntity;

import java.util.Map;

/**
 * 订单操作历史记录
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 11:22:13
 */
public interface OrderOperateHistoryService extends IService<OrderOperateHistoryEntity> {

    PageVO<OrderOperateHistoryEntity> queryPage(Map<String, Object> params);
}


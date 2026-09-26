package order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import order.entity.OrderOperateHistoryEntity;

import java.util.Map;

import common.query.PageQuery;
/**
 * 订单操作历史记录
 */
public interface OrderOperateHistoryService extends IService<OrderOperateHistoryEntity> {

    PageVO<OrderOperateHistoryEntity> queryPage(PageQuery query);
}


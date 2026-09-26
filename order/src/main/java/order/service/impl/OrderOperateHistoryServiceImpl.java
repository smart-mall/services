package order.service.impl;

import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;

import order.dao.OrderOperateHistoryDao;
import order.entity.OrderOperateHistoryEntity;
import order.service.OrderOperateHistoryService;


import common.query.PageQuery;
/**
 * 订单操作历史服务实现，提供操作历史的分页查询。
 *
 * <p>继承 MyBatis-Plus 的 {@code ServiceImpl}，无状态、线程安全。
 */
@Service("orderOperateHistoryService")
public class OrderOperateHistoryServiceImpl extends ServiceImpl<OrderOperateHistoryDao, OrderOperateHistoryEntity> implements OrderOperateHistoryService {

    /** {@inheritDoc} */
    @Override
    public PageVO<OrderOperateHistoryEntity> queryPage(PageQuery query) {
        IPage<OrderOperateHistoryEntity> page = this.page(query.toPage());

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

}
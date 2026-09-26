package order.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;

import order.dao.OrderItemDao;
import order.entity.OrderItemEntity;
import order.service.OrderItemService;


import common.query.PageQuery;
/**
 * 订单项服务实现，提供订单项的分页查询。
 *
 * <p>继承 MyBatis-Plus 的 {@code ServiceImpl}，无状态、线程安全。
 */
@Service("orderItemService")
public class OrderItemServiceImpl extends ServiceImpl<OrderItemDao, OrderItemEntity> implements OrderItemService {

    /** {@inheritDoc} */
    @Override
    public PageVO<OrderItemEntity> queryPage(PageQuery query) {
        IPage<OrderItemEntity> page = this.page(query.toPage());

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

}
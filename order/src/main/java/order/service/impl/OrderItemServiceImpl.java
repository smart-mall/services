package order.service.impl;

import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;

import order.dao.OrderItemDao;
import order.entity.OrderItemEntity;
import order.service.OrderItemService;


import common.query.PageQuery;
@Service("orderItemService")
public class OrderItemServiceImpl extends ServiceImpl<OrderItemDao, OrderItemEntity> implements OrderItemService {

    @Override
    public PageVO<OrderItemEntity> queryPage(PageQuery query) {
        IPage<OrderItemEntity> page = this.page(
                query.toPage(),
                new QueryWrapper<OrderItemEntity>()
        );

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

}
package order.service.impl;

import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;

import order.dao.OrderReturnReasonDao;
import order.entity.OrderReturnReasonEntity;
import order.service.OrderReturnReasonService;


import common.query.PageQuery;
@Service("orderReturnReasonService")
public class OrderReturnReasonServiceImpl extends ServiceImpl<OrderReturnReasonDao, OrderReturnReasonEntity> implements OrderReturnReasonService {

    @Override
    public PageVO<OrderReturnReasonEntity> queryPage(PageQuery query) {
        IPage<OrderReturnReasonEntity> page = this.page(
                query.toPage(),
                new QueryWrapper<OrderReturnReasonEntity>()
        );

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

}
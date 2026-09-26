package order.service.impl;

import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;

import order.dao.OrderReturnApplyDao;
import order.entity.OrderReturnApplyEntity;
import order.service.OrderReturnApplyService;


import common.query.PageQuery;
@Service("orderReturnApplyService")
public class OrderReturnApplyServiceImpl extends ServiceImpl<OrderReturnApplyDao, OrderReturnApplyEntity> implements OrderReturnApplyService {

    @Override
    public PageVO<OrderReturnApplyEntity> queryPage(PageQuery query) {
        IPage<OrderReturnApplyEntity> page = this.page(
                query.toPage(),
                new QueryWrapper<OrderReturnApplyEntity>()
        );

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

}
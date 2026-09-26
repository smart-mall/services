package order.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;

import order.dao.OrderReturnApplyDao;
import order.entity.OrderReturnApplyEntity;
import order.service.OrderReturnApplyService;


import common.query.PageQuery;
/**
 * 订单退货申请服务实现，提供退货申请的分页查询。
 *
 * <p>继承 MyBatis-Plus 的 {@code ServiceImpl}，无状态、线程安全。
 */
@Service("orderReturnApplyService")
public class OrderReturnApplyServiceImpl extends ServiceImpl<OrderReturnApplyDao, OrderReturnApplyEntity> implements OrderReturnApplyService {

    /** {@inheritDoc} */
    @Override
    public PageVO<OrderReturnApplyEntity> queryPage(PageQuery query) {
        IPage<OrderReturnApplyEntity> page = this.page(query.toPage());

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

}
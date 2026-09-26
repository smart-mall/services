package order.service.impl;

import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;

import order.dao.OrderReturnReasonDao;
import order.entity.OrderReturnReasonEntity;
import order.service.OrderReturnReasonService;


import common.query.PageQuery;
/**
 * 退货原因服务实现，提供退货原因的分页查询。
 *
 * <p>继承 MyBatis-Plus 的 {@code ServiceImpl}，无状态、线程安全。
 */
@Service("orderReturnReasonService")
public class OrderReturnReasonServiceImpl extends ServiceImpl<OrderReturnReasonDao, OrderReturnReasonEntity> implements OrderReturnReasonService {

    /** {@inheritDoc} */
    @Override
    public PageVO<OrderReturnReasonEntity> queryPage(PageQuery query) {
        IPage<OrderReturnReasonEntity> page = this.page(query.toPage());

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

}
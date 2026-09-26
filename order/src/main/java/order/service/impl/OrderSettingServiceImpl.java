package order.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;

import order.dao.OrderSettingDao;
import order.entity.OrderSettingEntity;
import order.service.OrderSettingService;


import common.query.PageQuery;
/**
 * 订单配置服务实现，提供订单配置的分页查询。
 *
 * <p>继承 MyBatis-Plus 的 {@code ServiceImpl}，无状态、线程安全。
 */
@Service("orderSettingService")
public class OrderSettingServiceImpl extends ServiceImpl<OrderSettingDao, OrderSettingEntity> implements OrderSettingService {

    /** {@inheritDoc} */
    @Override
    public PageVO<OrderSettingEntity> queryPage(PageQuery query) {
        IPage<OrderSettingEntity> page = this.page(query.toPage());

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

}
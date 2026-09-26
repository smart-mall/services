package order.service.impl;

import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;

import order.dao.OrderSettingDao;
import order.entity.OrderSettingEntity;
import order.service.OrderSettingService;


import common.query.PageQuery;
@Service("orderSettingService")
public class OrderSettingServiceImpl extends ServiceImpl<OrderSettingDao, OrderSettingEntity> implements OrderSettingService {

    @Override
    public PageVO<OrderSettingEntity> queryPage(PageQuery query) {
        IPage<OrderSettingEntity> page = this.page(
                query.toPage(),
                new QueryWrapper<OrderSettingEntity>()
        );

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

}
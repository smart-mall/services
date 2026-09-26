package ware.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;
import org.springframework.stereotype.Service;
import ware.dao.WareOrderTaskDao;
import ware.entity.WareOrderTaskEntity;
import ware.service.WareOrderTaskService;

import java.util.Map;


import common.query.PageQuery;
@Service("wareOrderTaskService")
public class WareOrderTaskServiceImpl extends ServiceImpl<WareOrderTaskDao, WareOrderTaskEntity> implements WareOrderTaskService {

    @Override
    public PageVO<WareOrderTaskEntity> queryPage(PageQuery query) {
        IPage<WareOrderTaskEntity> page = this.page(
                query.toPage(),
                new QueryWrapper<>()
        );

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

    @Override
    public WareOrderTaskEntity getOrderTaskByOrderSn(String orderSn) {

        return this.baseMapper.selectOne(
                new QueryWrapper<WareOrderTaskEntity>().eq("order_sn", orderSn));
    }

}
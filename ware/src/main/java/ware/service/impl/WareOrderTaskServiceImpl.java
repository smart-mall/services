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
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
/**
 * 库存工作单服务的默认实现，基于 MyBatis-Plus 的 {@code ServiceImpl} 读写 {@code wms_ware_order_task}。
 *
 * <p>工作单由锁定库存时创建，订单关闭事件到达时按订单号回查。
 */
@Service("wareOrderTaskService")
public class WareOrderTaskServiceImpl extends ServiceImpl<WareOrderTaskDao, WareOrderTaskEntity> implements WareOrderTaskService {

    /** {@inheritDoc} */
    @Override
    public PageVO<WareOrderTaskEntity> queryPage(PageQuery query) {
        IPage<WareOrderTaskEntity> page = this.page(query.toPage());

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

    /** {@inheritDoc} */
    @Override
    public WareOrderTaskEntity getOrderTaskByOrderSn(String orderSn) {

        return this.baseMapper.selectOne(
                new LambdaQueryWrapper<WareOrderTaskEntity>().eq(WareOrderTaskEntity::getOrderSn, orderSn));
    }

}
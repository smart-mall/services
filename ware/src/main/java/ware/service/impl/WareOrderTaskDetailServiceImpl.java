package ware.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;

import ware.dao.WareOrderTaskDetailDao;
import ware.entity.WareOrderTaskDetailEntity;
import ware.service.WareOrderTaskDetailService;


import common.query.PageQuery;
/**
 * 库存工作单明细服务的默认实现，基于 MyBatis-Plus 的 {@code ServiceImpl} 读写 {@code wms_ware_order_task_detail}。
 *
 * <p>锁定库存时逐条写入，解锁时按明细 ID 回写锁定状态，没有额外的业务编排。
 */
@Service("wareOrderTaskDetailService")
public class WareOrderTaskDetailServiceImpl extends ServiceImpl<WareOrderTaskDetailDao, WareOrderTaskDetailEntity> implements WareOrderTaskDetailService {

    /** {@inheritDoc} */
    @Override
    public PageVO<WareOrderTaskDetailEntity> queryPage(PageQuery query) {
        IPage<WareOrderTaskDetailEntity> page = this.page(query.toPage());

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

}
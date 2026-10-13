package ware.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import ware.dao.WareOrderTaskDetailDao;
import ware.entity.WareOrderTaskDetailEntity;
import ware.service.WareOrderTaskDetailService;

/**
 * 库存工作单明细服务的默认实现，基于 MyBatis-Plus 的 {@code ServiceImpl} 读写 {@code wms_ware_order_task_detail}。
 *
 * <p>锁定库存时逐条写入，解锁与出库实扣时按明细 ID 回写锁定状态，没有额外的业务编排。
 */
@Service("wareOrderTaskDetailService")
public class WareOrderTaskDetailServiceImpl extends ServiceImpl<WareOrderTaskDetailDao, WareOrderTaskDetailEntity> implements WareOrderTaskDetailService {
}

package ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import ware.entity.WareOrderTaskDetailEntity;

/**
 * 库存工作单明细服务：记录每个 SKU 锁在哪个仓库、锁了多少。
 *
 * <p>每行对应一次成功的库存锁定，{@code lockStatus} 为 1 表示已锁定、2 表示已解锁、3 表示扣减；
 * 解锁与出库实扣都只处理仍为"已锁定"的行，按明细 ID 定位。
 *
 * <p>明细不对后台单独开列表：它脱离工作单没有意义，由工作单列表一起返回。
 */
public interface WareOrderTaskDetailService extends IService<WareOrderTaskDetailEntity> {
}

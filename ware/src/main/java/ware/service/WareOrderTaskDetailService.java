package ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import ware.entity.WareOrderTaskDetailEntity;

import java.util.Map;

import common.query.PageQuery;
/**
 * 库存工作单明细服务：记录每个 SKU 锁在哪个仓库、锁了多少。
 *
 * <p>每行对应一次成功的库存锁定，{@code lockStatus} 为 1 表示已锁定、2 表示已解锁；
 * 订单解锁按明细 ID 定位，只处理仍为"已锁定"的行。
 */
public interface WareOrderTaskDetailService extends IService<WareOrderTaskDetailEntity> {

    /**
     * 分页查询库存工作单明细，不附加任何筛选条件。
     *
     * @param query 分页参数，不能为 {@code null}
     * @return 工作单明细分页数据；无数据时 {@code rows} 为空列表，{@code total} 为 0
     */
    PageVO<WareOrderTaskDetailEntity> queryPage(PageQuery query);
}


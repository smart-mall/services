package order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import order.entity.OrderOperateHistoryEntity;


import common.query.PageQuery;
/**
 * 订单操作历史服务：在通用 CRUD 之上提供后台列表的分页查询。
 */
public interface OrderOperateHistoryService extends IService<OrderOperateHistoryEntity> {

    /**
     * 分页查询订单操作历史，供后台列表使用。
     *
     * <p>没有业务筛选条件：任何订单的操作记录都会返回。
     *
     * @param query 分页参数，不能为 {@code null}
     * @return 分页结果，{@code rows} 为当前页记录；当前页没有数据时为空列表
     */
    PageVO<OrderOperateHistoryEntity> queryPage(PageQuery query);
}


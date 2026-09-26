package order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import order.entity.OrderReturnReasonEntity;


import common.query.PageQuery;
/** 退货原因服务：在通用 CRUD 之上提供后台列表的分页查询。 */
public interface OrderReturnReasonService extends IService<OrderReturnReasonEntity> {

    /**
     * 分页查询退货原因，供后台列表使用。
     *
     * <p>没有业务筛选条件：全部退货原因都会返回，不按启用状态过滤。
     *
     * @param query 分页参数，不能为 {@code null}
     * @return 分页结果，{@code rows} 为当前页退货原因；当前页没有数据时为空列表
     */
    PageVO<OrderReturnReasonEntity> queryPage(PageQuery query);
}


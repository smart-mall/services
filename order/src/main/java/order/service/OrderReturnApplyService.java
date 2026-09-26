package order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import order.entity.OrderReturnApplyEntity;


import common.query.PageQuery;
/** 退货申请服务：在通用 CRUD 之上提供后台列表的分页查询。 */
public interface OrderReturnApplyService extends IService<OrderReturnApplyEntity> {

    /**
     * 分页查询退货申请，供后台列表使用。
     *
     * <p>没有业务筛选条件：任何订单的退货申请都会返回。
     *
     * @param query 分页参数，不能为 {@code null}
     * @return 分页结果，{@code rows} 为当前页退货申请；当前页没有数据时为空列表
     */
    PageVO<OrderReturnApplyEntity> queryPage(PageQuery query);
}


package order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import order.entity.OrderReturnReasonEntity;

import java.util.Map;

import common.query.PageQuery;
/**
 * 退货原因
 */
public interface OrderReturnReasonService extends IService<OrderReturnReasonEntity> {

    PageVO<OrderReturnReasonEntity> queryPage(PageQuery query);
}


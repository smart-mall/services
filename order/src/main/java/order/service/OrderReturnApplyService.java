package order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import order.entity.OrderReturnApplyEntity;

import java.util.Map;

import common.query.PageQuery;
/**
 * 订单退货申请
 */
public interface OrderReturnApplyService extends IService<OrderReturnApplyEntity> {

    PageVO<OrderReturnApplyEntity> queryPage(PageQuery query);
}


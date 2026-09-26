package order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import order.entity.OrderReturnApplyEntity;

import java.util.Map;

/**
 * 订单退货申请
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 11:22:13
 */
public interface OrderReturnApplyService extends IService<OrderReturnApplyEntity> {

    PageVO<OrderReturnApplyEntity> queryPage(Map<String, Object> params);
}


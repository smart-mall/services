package order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import order.entity.OrderReturnReasonEntity;

import java.util.Map;

/**
 * 退货原因
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 11:22:13
 */
public interface OrderReturnReasonService extends IService<OrderReturnReasonEntity> {

    PageVO<OrderReturnReasonEntity> queryPage(Map<String, Object> params);
}


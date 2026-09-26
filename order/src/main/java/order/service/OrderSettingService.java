package order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import order.entity.OrderSettingEntity;

import java.util.Map;

import common.query.PageQuery;
/**
 * 订单配置信息
 */
public interface OrderSettingService extends IService<OrderSettingEntity> {

    PageVO<OrderSettingEntity> queryPage(PageQuery query);
}


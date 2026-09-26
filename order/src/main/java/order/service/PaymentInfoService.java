package order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import order.entity.PaymentInfoEntity;

import java.util.Map;

/**
 * 支付信息表
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 11:22:13
 */
public interface PaymentInfoService extends IService<PaymentInfoEntity> {

    PageVO<PaymentInfoEntity> queryPage(Map<String, Object> params);
}


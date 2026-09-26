package order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import order.entity.PaymentInfoEntity;

import java.util.Map;

import common.query.PageQuery;
/**
 * 支付信息表
 */
public interface PaymentInfoService extends IService<PaymentInfoEntity> {

    PageVO<PaymentInfoEntity> queryPage(PageQuery query);
}


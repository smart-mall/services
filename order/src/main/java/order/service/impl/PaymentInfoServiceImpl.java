package order.service.impl;

import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;

import order.dao.PaymentInfoDao;
import order.entity.PaymentInfoEntity;
import order.service.PaymentInfoService;


import common.query.PageQuery;
/**
 * 支付信息服务实现，提供支付信息的分页查询。
 *
 * <p>继承 MyBatis-Plus 的 {@code ServiceImpl}，无状态、线程安全。
 */
@Service("paymentInfoService")
public class PaymentInfoServiceImpl extends ServiceImpl<PaymentInfoDao, PaymentInfoEntity> implements PaymentInfoService {

    /** {@inheritDoc} */
    @Override
    public PageVO<PaymentInfoEntity> queryPage(PageQuery query) {
        IPage<PaymentInfoEntity> page = this.page(query.toPage());

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

}
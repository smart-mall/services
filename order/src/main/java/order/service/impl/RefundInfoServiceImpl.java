package order.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;

import order.dao.RefundInfoDao;
import order.entity.RefundInfoEntity;
import order.service.RefundInfoService;


import common.query.PageQuery;
/**
 * 退款信息服务实现，提供退款信息的分页查询。
 *
 * <p>继承 MyBatis-Plus 的 {@code ServiceImpl}，无状态、线程安全。
 */
@Service("refundInfoService")
public class RefundInfoServiceImpl extends ServiceImpl<RefundInfoDao, RefundInfoEntity> implements RefundInfoService {

    /** {@inheritDoc} */
    @Override
    public PageVO<RefundInfoEntity> queryPage(PageQuery query) {
        IPage<RefundInfoEntity> page = this.page(query.toPage());

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

}
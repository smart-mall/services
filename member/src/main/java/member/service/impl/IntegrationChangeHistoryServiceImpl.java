package member.service.impl;

import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;

import member.dao.IntegrationChangeHistoryDao;
import member.entity.IntegrationChangeHistoryEntity;
import member.service.IntegrationChangeHistoryService;


import common.query.PageQuery;
/**
 * 会员积分变动流水的查询实现。
 *
 * <p>无状态、线程安全，只做分页透传，不修改数据。
 */
@Service("integrationChangeHistoryService")
public class IntegrationChangeHistoryServiceImpl extends ServiceImpl<IntegrationChangeHistoryDao, IntegrationChangeHistoryEntity> implements IntegrationChangeHistoryService {

    /** {@inheritDoc} */
    @Override
    public PageVO<IntegrationChangeHistoryEntity> queryPage(PageQuery query) {
        IPage<IntegrationChangeHistoryEntity> page = this.page(query.toPage());

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

}
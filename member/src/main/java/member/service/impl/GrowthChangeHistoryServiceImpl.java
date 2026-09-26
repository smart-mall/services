package member.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;

import member.dao.GrowthChangeHistoryDao;
import member.entity.GrowthChangeHistoryEntity;
import member.service.GrowthChangeHistoryService;


import common.query.PageQuery;
/**
 * 会员成长值变动流水的查询实现。
 *
 * <p>无状态、线程安全，只做分页透传，不修改数据。
 */
@Service("growthChangeHistoryService")
public class GrowthChangeHistoryServiceImpl extends ServiceImpl<GrowthChangeHistoryDao, GrowthChangeHistoryEntity> implements GrowthChangeHistoryService {

    /** {@inheritDoc} */
    @Override
    public PageVO<GrowthChangeHistoryEntity> queryPage(PageQuery query) {
        IPage<GrowthChangeHistoryEntity> page = this.page(query.toPage());

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

}
package member.service.impl;

import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;

import member.dao.IntegrationChangeHistoryDao;
import member.entity.IntegrationChangeHistoryEntity;
import member.service.IntegrationChangeHistoryService;


import common.query.PageQuery;
@Service("integrationChangeHistoryService")
public class IntegrationChangeHistoryServiceImpl extends ServiceImpl<IntegrationChangeHistoryDao, IntegrationChangeHistoryEntity> implements IntegrationChangeHistoryService {

    @Override
    public PageVO<IntegrationChangeHistoryEntity> queryPage(PageQuery query) {
        IPage<IntegrationChangeHistoryEntity> page = this.page(
                query.toPage(),
                new QueryWrapper<IntegrationChangeHistoryEntity>()
        );

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

}
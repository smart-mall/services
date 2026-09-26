package member.service.impl;

import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;

import member.dao.GrowthChangeHistoryDao;
import member.entity.GrowthChangeHistoryEntity;
import member.service.GrowthChangeHistoryService;


import common.query.PageQuery;
@Service("growthChangeHistoryService")
public class GrowthChangeHistoryServiceImpl extends ServiceImpl<GrowthChangeHistoryDao, GrowthChangeHistoryEntity> implements GrowthChangeHistoryService {

    @Override
    public PageVO<GrowthChangeHistoryEntity> queryPage(PageQuery query) {
        IPage<GrowthChangeHistoryEntity> page = this.page(
                query.toPage(),
                new QueryWrapper<GrowthChangeHistoryEntity>()
        );

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

}
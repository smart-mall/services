package member.service.impl;

import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;

import member.dao.MemberStatisticsInfoDao;
import member.entity.MemberStatisticsInfoEntity;
import member.service.MemberStatisticsInfoService;


import common.query.PageQuery;
@Service("memberStatisticsInfoService")
public class MemberStatisticsInfoServiceImpl extends ServiceImpl<MemberStatisticsInfoDao, MemberStatisticsInfoEntity> implements MemberStatisticsInfoService {

    @Override
    public PageVO<MemberStatisticsInfoEntity> queryPage(PageQuery query) {
        IPage<MemberStatisticsInfoEntity> page = this.page(
                query.toPage(),
                new QueryWrapper<MemberStatisticsInfoEntity>()
        );

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

}
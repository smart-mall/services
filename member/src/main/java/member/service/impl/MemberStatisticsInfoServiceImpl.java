package member.service.impl;

import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;

import member.dao.MemberStatisticsInfoDao;
import member.entity.MemberStatisticsInfoEntity;
import member.service.MemberStatisticsInfoService;


import common.query.PageQuery;
/**
 * 会员统计信息的查询实现。
 *
 * <p>无状态、线程安全，只做分页透传，不修改数据。
 */
@Service("memberStatisticsInfoService")
public class MemberStatisticsInfoServiceImpl extends ServiceImpl<MemberStatisticsInfoDao, MemberStatisticsInfoEntity> implements MemberStatisticsInfoService {

    /** {@inheritDoc} */
    @Override
    public PageVO<MemberStatisticsInfoEntity> queryPage(PageQuery query) {
        IPage<MemberStatisticsInfoEntity> page = this.page(query.toPage());

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

}
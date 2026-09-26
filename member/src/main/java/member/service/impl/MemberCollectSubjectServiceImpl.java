package member.service.impl;

import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;

import member.dao.MemberCollectSubjectDao;
import member.entity.MemberCollectSubjectEntity;
import member.service.MemberCollectSubjectService;


import common.query.PageQuery;
/**
 * 会员专题活动收藏记录的查询实现。
 *
 * <p>无状态、线程安全，只做分页透传，不修改数据。
 */
@Service("memberCollectSubjectService")
public class MemberCollectSubjectServiceImpl extends ServiceImpl<MemberCollectSubjectDao, MemberCollectSubjectEntity> implements MemberCollectSubjectService {

    /** {@inheritDoc} */
    @Override
    public PageVO<MemberCollectSubjectEntity> queryPage(PageQuery query) {
        IPage<MemberCollectSubjectEntity> page = this.page(query.toPage());

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

}
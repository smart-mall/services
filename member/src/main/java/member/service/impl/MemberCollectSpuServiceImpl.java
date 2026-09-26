package member.service.impl;

import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;

import member.dao.MemberCollectSpuDao;
import member.entity.MemberCollectSpuEntity;
import member.service.MemberCollectSpuService;


import common.query.PageQuery;
/**
 * 会员商品收藏记录的查询实现。
 *
 * <p>无状态、线程安全，只做分页透传，不修改数据。
 */
@Service("memberCollectSpuService")
public class MemberCollectSpuServiceImpl extends ServiceImpl<MemberCollectSpuDao, MemberCollectSpuEntity> implements MemberCollectSpuService {

    /** {@inheritDoc} */
    @Override
    public PageVO<MemberCollectSpuEntity> queryPage(PageQuery query) {
        IPage<MemberCollectSpuEntity> page = this.page(query.toPage());

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

}
package member.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.to.LoginLogTo;
import common.vo.PageVO;
import member.dao.MemberLoginLogDao;
import member.entity.MemberLoginLogEntity;
import member.service.MemberLoginLogService;
import org.springframework.stereotype.Service;

import java.util.Date;


import common.query.PageQuery;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
/**
 * 会员登录记录的落库与查询实现。
 *
 * <p>无状态、线程安全；记录只追加不修改，登录时间取写入时刻。
 */
@Service("memberLoginLogService")
public class MemberLoginLogServiceImpl extends ServiceImpl<MemberLoginLogDao, MemberLoginLogEntity> implements MemberLoginLogService {

    /** {@inheritDoc} */
    @Override
    public PageVO<MemberLoginLogEntity> queryPage(PageQuery query) {
        IPage<MemberLoginLogEntity> page = this.page(query.toPage());

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

    /** {@inheritDoc} */
    @Override
    public void record(LoginLogTo to) {
        MemberLoginLogEntity entity = new MemberLoginLogEntity();
        entity.setMemberId(to.getMemberId());
        entity.setIp(to.getIp());
        entity.setCity(to.getCity());
        entity.setLoginType(to.getLoginType());
        entity.setCreateTime(new Date());

        this.save(entity);
    }

    /** {@inheritDoc} */
    @Override
    public PageVO<MemberLoginLogEntity> queryMine(Long memberId, PageQuery query) {
        IPage<MemberLoginLogEntity> page = this.page(
                query.toPage(),
                new LambdaQueryWrapper<MemberLoginLogEntity>()
                        .eq(MemberLoginLogEntity::getMemberId, memberId)
                        .orderByDesc(MemberLoginLogEntity::getCreateTime)
        );

        return new PageVO<>(page.getTotal(), page.getRecords());
    }


}

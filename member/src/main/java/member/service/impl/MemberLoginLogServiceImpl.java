package member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.exception.ValidationException;
import common.to.LoginLogTo;
import common.vo.PageVO;
import member.dao.MemberLoginLogDao;
import member.entity.MemberLoginLogEntity;
import member.service.MemberLoginLogService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;


import common.query.PageQuery;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
@Service("memberLoginLogService")
public class MemberLoginLogServiceImpl extends ServiceImpl<MemberLoginLogDao, MemberLoginLogEntity> implements MemberLoginLogService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    @Override
    public PageVO<MemberLoginLogEntity> queryPage(PageQuery query) {
        IPage<MemberLoginLogEntity> page = this.page(query.toPage());

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

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

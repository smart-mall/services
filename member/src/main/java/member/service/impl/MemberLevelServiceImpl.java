package member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;
import member.dao.MemberLevelDao;
import member.entity.MemberLevelEntity;
import member.service.MemberLevelService;
import member.vo.MemberSelectVO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;


import common.query.KeyPageQuery;
@Service("memberLevelService")
public class MemberLevelServiceImpl extends ServiceImpl<MemberLevelDao, MemberLevelEntity> implements MemberLevelService {

    @Override
    public PageVO<MemberLevelEntity> queryPage(KeyPageQuery query) {
        String key = query.getKey();

        LambdaQueryWrapper<MemberLevelEntity> wrapper = new LambdaQueryWrapper<>();

        if(key != null && !key.trim().isEmpty()){
            wrapper.like(MemberLevelEntity::getName, key)
                .or()
                .like(MemberLevelEntity::getId, key);
        }

        IPage<MemberLevelEntity> page = this.page(
                query.toPage(),
                wrapper
        );

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

    @Override
    public List<MemberSelectVO> getMemberSelect() {
        return baseMapper.selectList(null).stream().map(item -> {
            MemberSelectVO memberSelectVO = new MemberSelectVO();
            memberSelectVO.setId(item.getId());
            memberSelectVO.setName(item.getName());
            return memberSelectVO;
        }).toList();
    }

}
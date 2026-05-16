package member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.utils.PageUtils;
import common.utils.Query;
import member.dao.MemberLevelDao;
import member.entity.MemberLevelEntity;
import member.service.MemberLevelService;
import member.vo.MemberSelectVO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;


@Service("memberLevelService")
public class MemberLevelServiceImpl extends ServiceImpl<MemberLevelDao, MemberLevelEntity> implements MemberLevelService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String key = (String)params.get("key");

        LambdaQueryWrapper<MemberLevelEntity> wrapper = new LambdaQueryWrapper<>();

        if(key != null && !key.trim().isEmpty()){
            wrapper.like(MemberLevelEntity::getName, key)
                .or()
                .like(MemberLevelEntity::getId, key);
        }

        IPage<MemberLevelEntity> page = this.page(
                new Query<MemberLevelEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
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
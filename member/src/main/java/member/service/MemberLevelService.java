package member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import member.entity.MemberLevelEntity;
import member.vo.MemberSelectVO;

import java.util.List;
import java.util.Map;

import common.query.KeyPageQuery;
/**
 * 会员等级
 */
public interface MemberLevelService extends IService<MemberLevelEntity> {

    PageVO<MemberLevelEntity> queryPage(KeyPageQuery query);

    List<MemberSelectVO> getMemberSelect();
}


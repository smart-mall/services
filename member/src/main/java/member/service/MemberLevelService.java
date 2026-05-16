package member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.utils.PageUtils;
import member.entity.MemberLevelEntity;
import member.vo.MemberSelectVO;

import java.util.List;
import java.util.Map;

/**
 * 会员等级
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 11:14:17
 */
public interface MemberLevelService extends IService<MemberLevelEntity> {

    PageUtils queryPage(Map<String, Object> params);

    List<MemberSelectVO> getMemberSelect();
}


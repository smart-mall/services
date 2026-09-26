package member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import member.entity.MemberCollectSubjectEntity;

import java.util.Map;

/**
 * 会员收藏的专题活动
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 11:14:17
 */
public interface MemberCollectSubjectService extends IService<MemberCollectSubjectEntity> {

    PageVO<MemberCollectSubjectEntity> queryPage(Map<String, Object> params);
}


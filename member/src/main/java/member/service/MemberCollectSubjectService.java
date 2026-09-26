package member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import member.entity.MemberCollectSubjectEntity;

import java.util.Map;

import common.query.PageQuery;
/**
 * 会员收藏的专题活动
 */
public interface MemberCollectSubjectService extends IService<MemberCollectSubjectEntity> {

    PageVO<MemberCollectSubjectEntity> queryPage(PageQuery query);
}


package member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import member.entity.MemberCollectSpuEntity;

import java.util.Map;

import common.query.PageQuery;
/**
 * 会员收藏的商品
 */
public interface MemberCollectSpuService extends IService<MemberCollectSpuEntity> {

    PageVO<MemberCollectSpuEntity> queryPage(PageQuery query);
}


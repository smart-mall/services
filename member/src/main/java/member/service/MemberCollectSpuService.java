package member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import member.entity.MemberCollectSpuEntity;

import java.util.Map;

/**
 * 会员收藏的商品
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 11:14:17
 */
public interface MemberCollectSpuService extends IService<MemberCollectSpuEntity> {

    PageVO<MemberCollectSpuEntity> queryPage(Map<String, Object> params);
}


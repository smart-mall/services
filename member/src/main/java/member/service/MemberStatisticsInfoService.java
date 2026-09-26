package member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import member.entity.MemberStatisticsInfoEntity;

import java.util.Map;

import common.query.PageQuery;
/**
 * 会员统计信息
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 11:14:17
 */
public interface MemberStatisticsInfoService extends IService<MemberStatisticsInfoEntity> {

    PageVO<MemberStatisticsInfoEntity> queryPage(PageQuery query);
}


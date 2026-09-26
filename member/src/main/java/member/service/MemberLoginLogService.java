package member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.to.LoginLogTo;
import common.vo.PageVO;
import member.entity.MemberLoginLogEntity;

import java.util.Map;

/**
 * 会员登录记录
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 11:14:17
 */
public interface MemberLoginLogService extends IService<MemberLoginLogEntity> {

    PageVO<MemberLoginLogEntity> queryPage(Map<String, Object> params);

    /** 落一条登录记录。createTime 由这里填，调用方不用管 */
    void record(LoginLogTo to);

    /** 某个会员自己的登录记录，按时间倒序。参数 pageNum / pageSize */
    PageVO<MemberLoginLogEntity> queryMine(Long memberId, Map<String, Object> params);
}

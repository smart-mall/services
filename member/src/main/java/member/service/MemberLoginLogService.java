package member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.to.LoginLogTo;
import common.vo.PageVO;
import member.entity.MemberLoginLogEntity;

import java.util.Map;

import common.query.PageQuery;
/**
 * 会员登录记录
 */
public interface MemberLoginLogService extends IService<MemberLoginLogEntity> {

    PageVO<MemberLoginLogEntity> queryPage(PageQuery query);

    /** 落一条登录记录。createTime 由这里填，调用方不用管 */
    void record(LoginLogTo to);

    /** 某个会员自己的登录记录，按时间倒序。参数 page / limit */
    PageVO<MemberLoginLogEntity> queryMine(Long memberId, PageQuery query);
}

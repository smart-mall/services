package member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.utils.PageUtils;
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

    PageUtils queryPage(Map<String, Object> params);
}


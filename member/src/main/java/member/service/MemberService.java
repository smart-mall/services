package member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.utils.PageUtils;
import member.entity.MemberEntity;
import member.exception.EmailException;
import member.exception.PhoneException;
import member.exception.UsernameException;
import member.vo.MemberUserLoginVo;
import member.vo.MemberUserRegisterVo;
import member.vo.QQUserInfo;
import member.vo.SocialUser;

import java.util.Map;

/**
 * 会员
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 11:14:17
 */
public interface MemberService extends IService<MemberEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void register(MemberUserRegisterVo vo);

    void checkPhoneUnique(String phone) throws PhoneException;

    void checkUserNameUnique(String userName) throws UsernameException;

    void checkEmailUnique(String email) throws EmailException;

    MemberEntity login(MemberUserLoginVo vo);

    MemberEntity login(SocialUser socialUser) throws Exception;

    MemberEntity login(QQUserInfo qqUserInfo);

    /**
     * 按邮箱查会员，给「邮箱 + 验证码」登录用。
     * 验证码由 auth 侧校验，这里只负责把人取出来；查不到返回 null。
     */
    MemberEntity loginByEmail(String email);
}


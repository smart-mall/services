package member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import member.entity.MemberEntity;
import member.exception.UsernameException;
import member.vo.MemberProfileUpdateVo;
import member.vo.MemberUserRegisterVo;
import member.vo.QQUserInfo;
import member.vo.SocialUser;

import java.util.Map;

import common.query.PageQuery;
/**
 * 会员
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 11:14:17
 */
public interface MemberService extends IService<MemberEntity> {

    PageVO<MemberEntity> queryPage(PageQuery query);

    /**
     * 账号密码注册。
     *
     * @throws UsernameException 账号已被占用
     */
    void accountRegister(MemberUserRegisterVo vo);

    /**
     * 账号密码登录。账号不存在、没设过密码、密码不对，三种情况一律返回 null，
     * 由调用方统一转成"账号或密码错误"（不区分是账号错还是密码错，避免账号枚举）。
     */
    MemberEntity loginByUsername(String username, String password);

    /**
     * 邮箱验证码登录：按邮箱找人，查不到就用 {@code username} 建一个新账号。
     *
     * <p>老用户的 {@code username} 会被忽略 —— 这条链路是用邮箱识别身份的，
     * 用户填错账号也应该能登录。只有新建时才用得上它。</p>
     *
     * @throws UsernameException 新建时账号已被占用
     */
    MemberEntity loginOrRegisterByEmail(String username, String email);

    /**
     * 手机验证码登录，语义同 {@link #loginOrRegisterByEmail}，把邮箱换成手机号。
     *
     * @throws UsernameException 新建时账号已被占用
     */
    MemberEntity loginOrRegisterByMobile(String username, String mobile);

    MemberEntity login(SocialUser socialUser) throws Exception;

    MemberEntity login(QQUserInfo qqUserInfo);

    /**
     * 修改会员资料，返回更新后的会员。
     *
     * <p>只动 {@link member.vo.MemberProfileUpdateVo} 里那 7 个字段。等级、积分、成长值、
     * 启用状态、用户名都不在白名单里 —— 让用户改它们等于提权。</p>
     */
    MemberEntity updateProfile(Long memberId, MemberProfileUpdateVo vo);

    /**
     * 换绑手机号。
     *
     * @throws common.exception.BaseException {@code MOBILE_IN_USE} 该手机号已绑定其他账号
     */
    void changeMobile(Long memberId, String mobile);

    /**
     * 换绑邮箱。
     *
     * @throws common.exception.BaseException {@code EMAIL_IN_USE} 该邮箱已绑定其他账号
     */
    void changeEmail(Long memberId, String email);
}

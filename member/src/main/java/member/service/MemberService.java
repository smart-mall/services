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
 * 会员账号服务：注册、多链路登录、资料与联系方式的维护。
 *
 * <p>登录分三条链路：账号密码、邮箱/手机验证码、社交授权。三条链路各自识别身份，
 * 后两条建出的账号没有密码，不能走账号密码登录。
 */
public interface MemberService extends IService<MemberEntity> {

    /**
     * 分页查询全部会员。
     *
     * <p>管理端使用，不带筛选条件；返回的实体带密码密文，不能直接出网。
     *
     * @param query 分页参数，不能为 {@code null}；{@code page} / {@code limit} 非法时取默认值并截断上限
     * @return 分页结果，无数据时 {@code rows} 为空列表、{@code total} 为 0，不返回 {@code null}
     */
    PageVO<MemberEntity> queryPage(PageQuery query);

    /**
     * 以账号密码注册新会员。
     *
     * <p>只处理账号和密码：手机号、邮箱由各自的验证码链路负责。新账号取默认等级，
     * 昵称默认与账号同名。
     *
     * @param vo 注册入参，不能为 {@code null}；{@code username}、{@code password} 都必须非空
     * @throws UsernameException 账号已被占用
     */
    void accountRegister(MemberUserRegisterVo vo);

    /**
     * 账号密码登录。
     *
     * <p>账号不存在、没设过密码、密码不对三种情况一律返回 {@code null}，由调用方统一转成
     * "账号或密码错误"——不区分是账号错还是密码错，避免被用来枚举账号。
     *
     * @param username 登录账号，不能为 {@code null} 或空白
     * @param password 明文密码，可以为 {@code null}（按密码错误处理）
     * @return 登录成功的会员；上述三种失败情况均返回 {@code null}
     */
    MemberEntity loginByUsername(String username, String password);

    /**
     * 邮箱验证码登录：按邮箱找人，查不到就用 {@code username} 建一个新账号。
     *
     * <p>已有账号时 {@code username} 被忽略——这条链路用邮箱识别身份，用户填错账号也应该能登录；
     * 只有新建时才用得上它，且新建的账号没有密码，只能继续走验证码登录。
     *
     * @param username 新建时使用的账号，已有账号时忽略；新建时必须非空且未被占用
     * @param email 邮箱，登录标识，不能为 {@code null}
     * @return 命中或新建的会员，不会返回 {@code null}
     * @throws UsernameException 新建时账号已被占用
     */
    MemberEntity loginOrRegisterByEmail(String username, String email);

    /**
     * 手机验证码登录，语义同 {@link #loginOrRegisterByEmail}，把邮箱换成手机号。
     *
     * @param username 新建时使用的账号，已有账号时忽略；新建时必须非空且未被占用
     * @param mobile 手机号，登录标识，不能为 {@code null}
     * @return 命中或新建的会员，不会返回 {@code null}
     * @throws UsernameException 新建时账号已被占用
     */
    MemberEntity loginOrRegisterByMobile(String username, String mobile);

    /**
     * 微博授权登录：按 {@code uid} 找人，没有就拉取微博资料建档。
     *
     * <p>已有账号只刷新 {@code accessToken} 与有效期；新建的账号不带 username 和密码，
     * 只能走微博登录。
     *
     * @param socialUser 微博授权结果，不能为 {@code null}；{@code uid}、{@code access_token} 必须非空
     * @return 登录成功的会员；微博资料接口未返回 200 时，返回的实体尚未落库、{@code id} 为 {@code null}
     * @throws Exception 调用微博用户信息接口失败时抛出
     */
    MemberEntity login(SocialUser socialUser) throws Exception;

    /**
     * QQ 授权登录：按 {@code openId} 找人，没有就用 QQ 资料建档。
     *
     * <p>已有账号直接返回、不更新任何资料；新建的账号不带 username 和密码，只能走 QQ 登录。
     *
     * @param qqUserInfo QQ 互联返回的用户信息，不能为 {@code null}；{@code openId} 与 {@code genderType} 必须非空
     * @return 命中或新建的会员，不会返回 {@code null}
     */
    MemberEntity login(QQUserInfo qqUserInfo);

    /**
     * 修改会员资料，返回更新后的会员。
     *
     * <p>只动 {@link member.vo.MemberProfileUpdateVo} 里那 7 个字段，且是整体替换语义：
     * {@code vo} 中为 {@code null} 的字段会被清空。等级、积分、成长值、启用状态、用户名
     * 都不在白名单里 —— 让用户改它们等于提权。
     *
     * @param memberId 会员 ID，不能为 {@code null}
     * @param vo 资料入参，不能为 {@code null}
     * @return 更新后的会员
     * @throws common.exception.BaseException {@code NOT_LOGIN_EXCEPTION} 会员不存在（如账号已被删除）
     */
    MemberEntity updateProfile(Long memberId, MemberProfileUpdateVo vo);

    /**
     * 换绑手机号。
     *
     * <p>同一手机号不能绑到两个账号上：手机号是登录标识，重复绑定会让按手机号查人命中多行。
     *
     * @param memberId 会员 ID，不能为 {@code null}
     * @param mobile 新手机号，不能为 {@code null}
     * @throws common.exception.BaseException {@code MOBILE_IN_USE} 该手机号已绑定其他账号
     */
    void changeMobile(Long memberId, String mobile);

    /**
     * 换绑邮箱。
     *
     * <p>同一邮箱不能绑到两个账号上：邮箱是登录标识，重复绑定会让按邮箱查人命中多行。
     *
     * @param memberId 会员 ID，不能为 {@code null}
     * @param email 新邮箱，不能为 {@code null}
     * @throws common.exception.BaseException {@code EMAIL_IN_USE} 该邮箱已绑定其他账号
     */
    void changeEmail(Long memberId, String email);
}

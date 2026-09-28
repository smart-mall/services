package member.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.exception.BaseCodeEnum;
import common.exception.BaseException;
import common.utils.HttpUtils;
import common.vo.PageVO;
import lombok.extern.slf4j.Slf4j;
import member.dao.MemberDao;
import member.dao.MemberLevelDao;
import member.entity.MemberEntity;
import member.entity.MemberLevelEntity;
import member.exception.UsernameException;
import member.service.MemberService;
import member.vo.MemberProfileUpdateVo;
import member.vo.MemberUserRegisterVo;
import member.vo.QQUserInfo;
import member.vo.SocialUser;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;


import common.query.KeyPageQuery;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
/**
 * 会员账号服务的实现：注册、多链路登录、资料与联系方式维护。
 *
 * <p>无状态、线程安全；默认等级经 {@link MemberLevelDao} 查询，其余数据走 MyBatis-Plus。
 */
@Service("memberService")
@Slf4j
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {
    private final MemberLevelDao memberLevelDao;

    /**
     * 构造器注入会员等级查询，注册链路靠它取默认等级。
     *
     * @param memberLevelDao 会员等级查询，不能为 {@code null}
     */
    public MemberServiceImpl(MemberLevelDao memberLevelDao) {
        this.memberLevelDao = memberLevelDao;
    }

    /** {@inheritDoc} */
    @Override
    public PageVO<MemberEntity> queryPage(KeyPageQuery query) {
        String key = query.getKey();
        LambdaQueryWrapper<MemberEntity> wrapper = new LambdaQueryWrapper<>();

        if (key != null && !key.isEmpty()) {
            // 整体括起来：不加括号时后续再补条件会被 or 拆散，变成"或"掉全部筛选
            wrapper.and(inner -> inner.like(MemberEntity::getUsername, key)
                    .or().like(MemberEntity::getNickname, key)
                    .or().like(MemberEntity::getMobile, key)
                    .or().like(MemberEntity::getEmail, key));
        }

        IPage<MemberEntity> page = this.page(query.toPage(), wrapper);

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

    /** {@inheritDoc} */
    @Override
    public Map<Long, String> getMemberNames(List<Long> memberIds) {
        // 必须先判空：空集合会让 SQL 拼成 IN ()，MySQL 直接报语法错
        if (memberIds == null || memberIds.isEmpty()) {
            return Map.of();
        }

        return this.listByIds(memberIds).stream()
                .filter(member -> member.getNickname() != null)
                .collect(Collectors.toMap(MemberEntity::getId, MemberEntity::getNickname, (first, second) -> first));
    }

    /** {@inheritDoc} */
    @Override
    public void accountRegister(MemberUserRegisterVo vo) {

        // 账号被占用直接抛出，由 controller 统一转成 15001
        checkUsernameAvailable(vo.getUsername());

        MemberEntity memberEntity = new MemberEntity();
        fillDefaults(memberEntity, vo.getUsername());
        memberEntity.setPassword(new BCryptPasswordEncoder().encode(vo.getPassword()));

        this.baseMapper.insert(memberEntity);
    }

    /** {@inheritDoc} */
    @Override
    public MemberEntity loginByUsername(String username, String password) {

        MemberEntity memberEntity = this.baseMapper.selectOne(
                new LambdaQueryWrapper<MemberEntity>().eq(MemberEntity::getUsername, username));

        if (memberEntity == null) {
            return null;
        }

        String passwordDB = memberEntity.getPassword();
        // 无密码账号（验证码链路建的）一律登录失败；先判空是因为 BCrypt.matches
        // 对 null 密文会打 "Empty encoded password" 的 warn 日志，正常业务不该刷
        if (StringUtils.isBlank(password) || passwordDB == null) {
            return null;
        }

        if (new BCryptPasswordEncoder().matches(password, passwordDB)) {
            return memberEntity;
        }

        return null;
    }

    /** {@inheritDoc} */
    @Override
    public MemberEntity loginOrRegisterByEmail(String username, String email) {
        return loginOrRegister(username, new LambdaQueryWrapper<MemberEntity>().eq(MemberEntity::getEmail, email),
                member -> member.setEmail(email));
    }

    /** {@inheritDoc} */
    @Override
    public MemberEntity loginOrRegisterByMobile(String username, String mobile) {
        return loginOrRegister(username, new LambdaQueryWrapper<MemberEntity>().eq(MemberEntity::getMobile, mobile),
                member -> member.setMobile(mobile));
    }

    /**
     * 验证码链路的公共实现：先按联系方式找人，找不到就用 {@code username} 建一个。
     *
     * <p>「查不到才建」本身就是联系方式的唯一性检查，不必再查一次；账号是用户填的、与联系方式无关，
     * 必须单独校验。新建的账号没有密码，只能靠验证码登录。
     *
     * @param username 新建时使用的账号
     * @param byContact 按联系方式查询的条件
     * @param bindContact 新建时把联系方式落到实体上
     * @return 命中的已有会员，或刚落库的新会员
     */
    private MemberEntity loginOrRegister(String username, LambdaQueryWrapper<MemberEntity> byContact,
                                         Consumer<MemberEntity> bindContact) {

        MemberEntity memberEntity = this.baseMapper.selectOne(byContact);
        if (memberEntity != null) {
            // 已有账号时忽略 username：这条链路按联系方式认人
            return memberEntity;
        }

        // 新建时账号是用户填的、全局唯一，撞了就抛给上层转 15001
        checkUsernameAvailable(username);

        MemberEntity register = new MemberEntity();
        fillDefaults(register, username);
        bindContact.accept(register);

        this.baseMapper.insert(register);
        return register;
    }

    /**
     * 校验用户名是否可用，已被占用时抛 {@link UsernameException}。
     *
     * @param userName 待校验的账号，不能为 {@code null}
     * @throws UsernameException 该账号已被占用
     */
    private void checkUsernameAvailable(String userName) {

        Long usernameCount = this.baseMapper.selectCount(
                new LambdaQueryWrapper<MemberEntity>().eq(MemberEntity::getUsername, userName));

        if (usernameCount > 0) {
            throw new UsernameException();
        }
    }

    /**
     * 填充注册共用的默认字段：默认等级、昵称、性别与注册时间。
     *
     * @param memberEntity 待填充的会员实体，不能为 {@code null}
     * @param userName 账号，同时用作默认昵称
     */
    private void fillDefaults(MemberEntity memberEntity, String userName) {

        MemberLevelEntity levelEntity = memberLevelDao.getDefaultLevel();
        if (levelEntity != null) {
            memberEntity.setLevelId(levelEntity.getId());
        } else {
            // 没有配默认等级不该让注册整个失败，落个 null 让后台能看出来
            log.warn("没有查询到默认会员等级，levelId 将为空");
        }

        memberEntity.setNickname(userName);
        memberEntity.setUsername(userName);
        // 0 表示性别未知，取值约定与资料修改接口一致
        memberEntity.setGender(0);
        memberEntity.setCreateTime(new Date());
    }

    /** {@inheritDoc} */
    @Override
    public MemberEntity login(SocialUser socialUser) throws Exception {

        String uid = socialUser.getUid();

        // 1. 按 socialUid 判断该微博账号是否已注册
        MemberEntity memberEntity = this.baseMapper.selectOne(new LambdaQueryWrapper<MemberEntity>().eq(MemberEntity::getSocialUid, uid));

        if (memberEntity != null) {
            // 已注册：刷新 access_token 与有效期，后续调微博接口要用
            MemberEntity update = new MemberEntity();
            update.setId(memberEntity.getId());
            update.setAccessToken(socialUser.getAccess_token());
            update.setExpiresIn(String.valueOf(socialUser.getExpires_in()));
            this.baseMapper.updateById(update);

            memberEntity.setAccessToken(socialUser.getAccess_token());
            memberEntity.setExpiresIn(String.valueOf(socialUser.getExpires_in()));
            return memberEntity;
        } else {
            // 2. 未注册：拉取微博资料后建档
            MemberEntity register = new MemberEntity();
            Map<String,String> query = new HashMap<>();
            query.put("access_token",socialUser.getAccess_token());
            query.put("uid",socialUser.getUid());
            HttpResponse response = HttpUtils.doGet("https://api.weibo.com", "/2/users/show.json", "get", new HashMap<String, String>(), query);

            if (response.getStatusLine().getStatusCode() == 200) {
                String json = EntityUtils.toString(response.getEntity());
                JSONObject jsonObject = JSON.parseObject(json);
                String name = jsonObject.getString("name");
                String gender = jsonObject.getString("gender");
                String profileImageUrl = jsonObject.getString("profile_image_url");

                register.setNickname(name);
                register.setGender("m".equals(gender)?1:0);
                register.setHeader(profileImageUrl);
                register.setCreateTime(new Date());
                register.setSocialUid(socialUser.getUid());
                register.setAccessToken(socialUser.getAccess_token());
                register.setExpiresIn(String.valueOf(socialUser.getExpires_in()));

                this.baseMapper.insert(register);

            }
            // 微博资料接口非 200 时不落库，返回的实体 id 为 null
            return register;
        }

    }

    /** {@inheritDoc} */
    @Override
    public MemberEntity login(QQUserInfo qqUserInfo) {
        String openid = qqUserInfo.getOpenId();

        MemberEntity memberEntity = this.baseMapper.selectOne(
                new LambdaQueryWrapper<>(MemberEntity.class).eq( MemberEntity::getSocialUid, openid)
        );

        if (memberEntity == null) {
            log.debug("新用户注册");
            memberEntity = new MemberEntity();
            memberEntity.setNickname(qqUserInfo.getNickname());
            memberEntity.setGender(Double.valueOf(qqUserInfo.getGenderType()).intValue());
            memberEntity.setHeader(qqUserInfo.getFigureurl());
            memberEntity.setCreateTime(new Date());
            memberEntity.setSocialUid(openid);
            this.baseMapper.insert(memberEntity);
        }
        return memberEntity;
    }

    /** {@inheritDoc} */
    @Override
    public MemberEntity updateProfile(Long memberId, MemberProfileUpdateVo vo) {

        // 用 set 逐列写：updateById 默认忽略 null 字段，用户清空职业/签名时会静默保留旧值。
        // 这串 set 同时就是安全边界 —— 只有这 7 列能被写进来
        this.update(new LambdaUpdateWrapper<MemberEntity>()
                .eq(MemberEntity::getId, memberId)
                .set(MemberEntity::getNickname, vo.getNickname())
                .set(MemberEntity::getHeader, vo.getHeader())
                .set(MemberEntity::getGender, vo.getGender())
                .set(MemberEntity::getBirth, vo.getBirth())
                .set(MemberEntity::getCity, vo.getCity())
                .set(MemberEntity::getJob, vo.getJob())
                .set(MemberEntity::getSign, vo.getSign()));

        MemberEntity updated = this.getById(memberId);
        if (updated == null) {
            // 令牌有效但库里没人：账号被删了，等同于未登录
            throw new BaseException(BaseCodeEnum.NOT_LOGIN_EXCEPTION);
        }
        return updated;
    }

    /** {@inheritDoc} */
    // 检查与写入放同一事务，但并发下仍有窗口：根治要加唯一索引，见 existsOnOther
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void changeMobile(Long memberId, String mobile) {
        if (existsOnOther(MemberEntity::getMobile, mobile, memberId)) {
            throw new BaseException(BaseCodeEnum.MOBILE_IN_USE);
        }
        this.update(new LambdaUpdateWrapper<MemberEntity>()
                .eq(MemberEntity::getId, memberId)
                .set(MemberEntity::getMobile, mobile));
    }

    /** {@inheritDoc} */
    // 检查与写入放同一事务，但并发下仍有窗口：根治要加唯一索引，见 existsOnOther
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void changeEmail(Long memberId, String email) {
        if (existsOnOther(MemberEntity::getEmail, email, memberId)) {
            throw new BaseException(BaseCodeEnum.EMAIL_IN_USE);
        }
        this.update(new LambdaUpdateWrapper<MemberEntity>()
                .eq(MemberEntity::getId, memberId)
                .set(MemberEntity::getEmail, email));
    }

    /**
     * 判断这个联系方式是否已经绑在别人身上。
     *
     * <p>必须查：mobile / email 都没有唯一索引，而按联系方式找人用的是 {@code selectOne}，
     * 写进重复值会让那个号从此登录抛 {@code TooManyResultsException}；并发下仍有窗口，
     * 根治要加唯一索引。排除自己是为了让"设成当前值"不报错；邮箱比较忽略大小写，
     * 由表的 utf8mb4_unicode_ci 排序规则保证，不用单独处理。
     *
     * @param column 联系方式列，取 {@code mobile} 或 {@code email}
     * @param value 待校验的联系方式，不能为 {@code null}
     * @param selfId 当前会员 ID，用于排除自己，不能为 {@code null}
     * @return {@code true} 表示该联系方式已绑在别的会员上
     */
    private boolean existsOnOther(SFunction<MemberEntity, ?> column, Object value, Long selfId) {
        Long count = this.baseMapper.selectCount(new LambdaQueryWrapper<MemberEntity>()
                .eq(column, value)
                .ne(MemberEntity::getId, selfId));
        return count != null && count > 0;
    }


}

package member.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
import java.util.Map;
import java.util.function.Consumer;


import common.query.PageQuery;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
@Service("memberService")
@Slf4j
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {
    private final MemberLevelDao memberLevelDao;

    public MemberServiceImpl(MemberLevelDao memberLevelDao) {
        this.memberLevelDao = memberLevelDao;
    }

    @Override
    public PageVO<MemberEntity> queryPage(PageQuery query) {
        IPage<MemberEntity> page = this.page(query.toPage());

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

    @Override
    public void accountRegister(MemberUserRegisterVo vo) {

        //感知异常，异常机制：账号被占用就抛，由 controller 转成 15001
        checkUsernameAvailable(vo.getUsername());

        MemberEntity memberEntity = new MemberEntity();
        fillDefaults(memberEntity, vo.getUsername());
        //密码进行BCrypt加密
        memberEntity.setPassword(new BCryptPasswordEncoder().encode(vo.getPassword()));

        //保存数据
        this.baseMapper.insert(memberEntity);
    }

    @Override
    public MemberEntity loginByUsername(String username, String password) {

        MemberEntity memberEntity = this.baseMapper.selectOne(
                new LambdaQueryWrapper<MemberEntity>().eq(MemberEntity::getUsername, username));

        if (memberEntity == null) {
            //账号不存在
            return null;
        }

        String passwordDB = memberEntity.getPassword();
        //没设过密码的账号（验证码链路建出来的）在这里一律登录失败。
        //先判空是必要的：BCryptPasswordEncoder.matches 遇到 null 的密文虽然不会抛异常，
        //但会打一条 "Empty encoded password" 的 warn 日志，正常业务不该刷这个日志。
        if (StringUtils.isBlank(password) || passwordDB == null) {
            return null;
        }

        //进行密码匹配
        if (new BCryptPasswordEncoder().matches(password, passwordDB)) {
            return memberEntity;
        }

        return null;
    }

    @Override
    public MemberEntity loginOrRegisterByEmail(String username, String email) {
        return loginOrRegister(username, new LambdaQueryWrapper<MemberEntity>().eq(MemberEntity::getEmail, email),
                member -> member.setEmail(email));
    }

    @Override
    public MemberEntity loginOrRegisterByMobile(String username, String mobile) {
        return loginOrRegister(username, new LambdaQueryWrapper<MemberEntity>().eq(MemberEntity::getMobile, mobile),
                member -> member.setMobile(mobile));
    }

    /**
     * 验证码链路的公共实现：先按联系方式找人，找不到就用 {@code username} 建一个。
     *
     * <p>「查不到才建」这一步本身就是唯一性检查，所以不需要再单独 check 邮箱/手机号是否重复；
     * 但账号是用户填的、和联系方式无关，必须单独查一次。</p>
     *
     * <p>建出来的账号<b>没有密码</b>：它只能靠验证码登录。这是有意的 ——
     * 验证码链路和账号密码链路是各自独立的两套，不互相授予登录能力。</p>
     *
     * @param byContact 按联系方式查询的条件
     * @param bindContact 新建时把联系方式落到实体上
     */
    private MemberEntity loginOrRegister(String username, LambdaQueryWrapper<MemberEntity> byContact,
                                         Consumer<MemberEntity> bindContact) {

        MemberEntity memberEntity = this.baseMapper.selectOne(byContact);
        if (memberEntity != null) {
            //老用户：username 忽略，这条链路是按联系方式认人的
            return memberEntity;
        }

        //新用户：账号是用户填的、全局唯一，撞了就抛给上层转 15001
        checkUsernameAvailable(username);

        MemberEntity register = new MemberEntity();
        fillDefaults(register, username);
        bindContact.accept(register);

        this.baseMapper.insert(register);
        return register;
    }

    /** 用户名是否已被占用，占用则抛 {@link UsernameException} */
    private void checkUsernameAvailable(String userName) {

        Long usernameCount = this.baseMapper.selectCount(
                new LambdaQueryWrapper<MemberEntity>().eq(MemberEntity::getUsername, userName));

        if (usernameCount > 0) {
            throw new UsernameException();
        }
    }

    /** 三条注册/自动注册链路共用的默认字段 */
    private void fillDefaults(MemberEntity memberEntity, String userName) {

        //设置默认等级
        MemberLevelEntity levelEntity = memberLevelDao.getDefaultLevel();
        if (levelEntity != null) {
            memberEntity.setLevelId(levelEntity.getId());
        } else {
            //没有配默认等级不该让注册整个失败，落个 null 让后台能看出来
            log.warn("没有查询到默认会员等级，levelId 将为空");
        }

        //设置昵称
        memberEntity.setNickname(userName);
        memberEntity.setUsername(userName);
        memberEntity.setGender(0);
        memberEntity.setCreateTime(new Date());
    }

    @Override
    public MemberEntity login(SocialUser socialUser) throws Exception {

        //具有登录和注册逻辑
        String uid = socialUser.getUid();

        //1、判断当前社交用户是否已经登录过系统
        MemberEntity memberEntity = this.baseMapper.selectOne(new LambdaQueryWrapper<MemberEntity>().eq(MemberEntity::getSocialUid, uid));

        if (memberEntity != null) {
            //这个用户已经注册过
            //更新用户的访问令牌的时间和access_token
            MemberEntity update = new MemberEntity();
            update.setId(memberEntity.getId());
            update.setAccessToken(socialUser.getAccess_token());
            update.setExpiresIn(String.valueOf(socialUser.getExpires_in()));
            this.baseMapper.updateById(update);

            memberEntity.setAccessToken(socialUser.getAccess_token());
            memberEntity.setExpiresIn(String.valueOf(socialUser.getExpires_in()));
            return memberEntity;
        } else {
            //2、没有查到当前社交用户对应的记录我们就需要注册一个
            MemberEntity register = new MemberEntity();
            //3、查询当前社交用户的社交账号信息（昵称、性别等）
            Map<String,String> query = new HashMap<>();
            query.put("access_token",socialUser.getAccess_token());
            query.put("uid",socialUser.getUid());
            HttpResponse response = HttpUtils.doGet("https://api.weibo.com", "/2/users/show.json", "get", new HashMap<String, String>(), query);

            if (response.getStatusLine().getStatusCode() == 200) {
                //查询成功
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

                //把用户信息插入到数据库中
                this.baseMapper.insert(register);

            }
            return register;
        }

    }

    @Override
    public MemberEntity login(QQUserInfo qqUserInfo) {
        String openid = qqUserInfo.getOpenId();

        MemberEntity memberEntity = this.baseMapper.selectOne(
                new LambdaQueryWrapper<>(MemberEntity.class).eq( MemberEntity::getSocialUid, openid)
        );

        if (memberEntity == null) {
            log.debug("新用户注册");
            //把扫码人的信息添加到数据库中
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
     * 这个联系方式是否已经绑在别人身上。
     *
     * <p>排除自己：改成和当前一样的值不该报"已绑定其他账号"。</p>
     *
     * <p>为什么要查：库里 mobile / email 都没有唯一索引，而按联系方式找人用的是
     * {@code selectOne} —— 一旦写进重复值，那个号从此登录会抛 TooManyResultsException。
     * 并发下仍有窗口，根治得加唯一索引（历史数据可能已有重复，加之前要先查一遍）。</p>
     *
     * <p>邮箱不用单独处理大小写：表是 utf8mb4_unicode_ci，比较本身就忽略大小写。</p>
     */
    private boolean existsOnOther(SFunction<MemberEntity, ?> column, Object value, Long selfId) {
        Long count = this.baseMapper.selectCount(new LambdaQueryWrapper<MemberEntity>()
                .eq(column, value)
                .ne(MemberEntity::getId, selfId));
        return count != null && count > 0;
    }


}

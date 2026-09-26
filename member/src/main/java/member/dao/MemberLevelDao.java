package member.dao;

import member.entity.MemberLevelEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * {@code ums_member_level} 表的 MyBatis-Plus Mapper，映射会员等级 {@link MemberLevelEntity}。
 *
 * <p>通用增删改查由 {@link BaseMapper} 提供；{@link #getDefaultLevel()} 的 SQL 写在
 * {@code mapper/member/MemberLevelDao.xml} 中。
 */
@Mapper
public interface MemberLevelDao extends BaseMapper<MemberLevelEntity> {

    /**
     * 查询默认会员等级。
     *
     * <p>筛选条件为 {@code default_status = 1}，注册时用它确定新会员的等级。
     *
     * @return 默认等级；未配置默认等级时返回 {@code null}，调用方需自行判空
     */
    MemberLevelEntity getDefaultLevel();
}

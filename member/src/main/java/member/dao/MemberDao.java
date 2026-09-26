package member.dao;

import member.entity.MemberEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * {@code ums_member} 表的 MyBatis-Plus Mapper，映射会员账号 {@link MemberEntity}。
 *
 * <p>未声明自定义 SQL，仅使用 {@link BaseMapper} 提供的通用增删改查。
 */
@Mapper
public interface MemberDao extends BaseMapper<MemberEntity> {
	
}

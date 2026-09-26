package coupon.dao;

import coupon.entity.HomeSubjectSpuEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * {@code sms_home_subject_spu} 表的 MyBatis-Plus Mapper，映射首页专题与商品 SPU 的关联 {@link HomeSubjectSpuEntity}。
 *
 * <p>未声明自定义 SQL，仅使用 {@link BaseMapper} 提供的通用增删改查。
 */
@Mapper
public interface HomeSubjectSpuDao extends BaseMapper<HomeSubjectSpuEntity> {
	
}

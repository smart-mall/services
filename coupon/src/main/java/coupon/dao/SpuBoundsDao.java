package coupon.dao;

import coupon.entity.SpuBoundsEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * {@code sms_spu_bounds} 表的 MyBatis-Plus Mapper，映射商品 SPU 积分设置 {@link SpuBoundsEntity}。
 *
 * <p>未声明自定义 SQL，仅使用 {@link BaseMapper} 提供的通用增删改查。
 */
@Mapper
public interface SpuBoundsDao extends BaseMapper<SpuBoundsEntity> {
	
}

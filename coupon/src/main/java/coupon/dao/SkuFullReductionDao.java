package coupon.dao;

import coupon.entity.SkuFullReductionEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * {@code sms_sku_full_reduction} 表的 MyBatis-Plus Mapper，映射商品满减规则 {@link SkuFullReductionEntity}。
 *
 * <p>未声明自定义 SQL，仅使用 {@link BaseMapper} 提供的通用增删改查。
 */
@Mapper
public interface SkuFullReductionDao extends BaseMapper<SkuFullReductionEntity> {
	
}

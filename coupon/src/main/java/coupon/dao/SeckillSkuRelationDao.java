package coupon.dao;

import coupon.entity.SeckillSkuRelationEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * {@code sms_seckill_sku_relation} 表的 MyBatis-Plus Mapper，映射秒杀活动与商品的关联 {@link SeckillSkuRelationEntity}。
 *
 * <p>未声明自定义 SQL，仅使用 {@link BaseMapper} 提供的通用增删改查。
 */
@Mapper
public interface SeckillSkuRelationDao extends BaseMapper<SeckillSkuRelationEntity> {
	
}

package coupon.dao;

import coupon.entity.SeckillPromotionEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * {@code sms_seckill_promotion} 表的 MyBatis-Plus Mapper，映射秒杀活动 {@link SeckillPromotionEntity}。
 *
 * <p>未声明自定义 SQL，仅使用 {@link BaseMapper} 提供的通用增删改查。
 */
@Mapper
public interface SeckillPromotionDao extends BaseMapper<SeckillPromotionEntity> {
	
}

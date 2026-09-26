package coupon.dao;

import coupon.entity.SeckillSkuNoticeEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * {@code sms_seckill_sku_notice} 表的 MyBatis-Plus Mapper，映射秒杀商品通知订阅 {@link SeckillSkuNoticeEntity}。
 *
 * <p>未声明自定义 SQL，仅使用 {@link BaseMapper} 提供的通用增删改查。
 */
@Mapper
public interface SeckillSkuNoticeDao extends BaseMapper<SeckillSkuNoticeEntity> {
	
}

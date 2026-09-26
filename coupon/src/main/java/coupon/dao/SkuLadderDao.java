package coupon.dao;

import coupon.entity.SkuLadderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * {@code sms_sku_ladder} 表的 MyBatis-Plus Mapper，映射商品阶梯价 {@link SkuLadderEntity}。
 *
 * <p>未声明自定义 SQL，仅使用 {@link BaseMapper} 提供的通用增删改查。
 */
@Mapper
public interface SkuLadderDao extends BaseMapper<SkuLadderEntity> {
	
}

package coupon.dao;

import coupon.entity.CouponEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * {@code sms_coupon} 表的 MyBatis-Plus Mapper，映射优惠券模板 {@link CouponEntity}。
 *
 * <p>未声明自定义 SQL，仅使用 {@link BaseMapper} 提供的通用增删改查。
 */
@Mapper
public interface CouponDao extends BaseMapper<CouponEntity> {
	
}

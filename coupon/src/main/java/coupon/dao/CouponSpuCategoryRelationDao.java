package coupon.dao;

import coupon.entity.CouponSpuCategoryRelationEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * {@code sms_coupon_spu_category_relation} 表的 MyBatis-Plus Mapper，映射优惠券与商品分类的关联 {@link CouponSpuCategoryRelationEntity}。
 *
 * <p>未声明自定义 SQL，仅使用 {@link BaseMapper} 提供的通用增删改查。
 */
@Mapper
public interface CouponSpuCategoryRelationDao extends BaseMapper<CouponSpuCategoryRelationEntity> {
	
}

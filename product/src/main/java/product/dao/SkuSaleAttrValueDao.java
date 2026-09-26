package product.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import product.entity.SkuSaleAttrValueEntity;
import product.vo.SkuItemSaleAttrVo;

import java.util.List;

/**
 * {@code pms_sku_sale_attr_value} 表的 MyBatis-Plus Mapper，无自定义 SQL。
 */
@Mapper
public interface SkuSaleAttrValueDao extends BaseMapper<SkuSaleAttrValueEntity> {

    List<SkuItemSaleAttrVo> getSaleAttrBySpuId(@Param("spuId") Long spuId);

    List<String> getSkuSaleAttrValuesAsStringList(@Param("skuId") Long skuId);
}

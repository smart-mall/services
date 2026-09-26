package product.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import product.entity.AttrGroupEntity;
import product.vo.SpuItemAttrGroupVo;

import java.util.List;

/**
 * {@code pms_attr_group} 表的 MyBatis-Plus Mapper，无自定义 SQL。
 */
@Mapper
public interface AttrGroupDao extends BaseMapper<AttrGroupEntity> {

    List<SpuItemAttrGroupVo> getAttrGroupWithAttrsBySpuId(Long spuId, Long catalogId);
}

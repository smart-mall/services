package product.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import product.entity.SpuInfoEntity;

/**
 * {@code pms_spu_info} 表的 MyBatis-Plus Mapper，无自定义 SQL。
 */
@Mapper
public interface SpuInfoDao extends BaseMapper<SpuInfoEntity> {

    void updateSpuStatus(Long spuId, Integer code);
}

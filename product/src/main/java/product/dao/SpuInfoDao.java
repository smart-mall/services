package product.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import product.entity.SpuInfoEntity;

/**
 * {@code pms_spu_info} SPU 主表的 MyBatis-Plus Mapper，自定义 SQL 写在 {@code SpuInfoDao.xml}。
 */
@Mapper
public interface SpuInfoDao extends BaseMapper<SpuInfoEntity> {

    /**
     * 更新 SPU 的上架状态。
     *
     * <p>SQL：{@code update pms_spu_info set publish_status = #{code}, update_time = now() where id = #{spuId}}，
     * 由上下架流程在远程调用完成后回写。
     *
     * @param spuId SPU 主键，不能为 {@code null}，否则条件不成立、更新 0 行
     * @param code 上架状态码，取值见 {@code ProductConstant.ProductStatusEnum}
     */
    void updateSpuStatus(Long spuId, Integer code);
}

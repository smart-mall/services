package ware.dao;

import org.apache.ibatis.annotations.Param;
import ware.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 商品库存
 * 
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 11:20:17
 */
@Mapper
public interface WareSkuDao extends BaseMapper<WareSkuEntity> {

    Long getSkuStock(Long skuId);

    void addStock(@Param("skuId") Long skuId, @Param("wareId") Long wareId, @Param("skuNum") Integer skuNum);

    Long lockSkuStock(Long skuId, Long wareId, Integer num);

    void unLockStock(Long skuId, Long wareId, Integer num);

    List<Long> listWareIdHasSkuStock(Long skuId);

}

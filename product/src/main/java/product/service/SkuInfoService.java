package product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import product.entity.SkuInfoEntity;
import product.vo.SkuItemVo;
import product.vo.SkuSelectVO;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import common.query.PageQuery;
import product.vo.SkuInfoPageQuery;
/**
 * sku信息
 */
public interface SkuInfoService extends IService<SkuInfoEntity> {

    PageVO<SkuInfoEntity> queryPage(PageQuery query);

    PageVO<SkuInfoEntity> queryPageByCondition(SkuInfoPageQuery query);

    SkuItemVo item(Long skuId) throws ExecutionException, InterruptedException;

    List<SkuSelectVO> getSkuSelect();

    Map<Long, String> getUserNames(List<Long> spuIds);
}


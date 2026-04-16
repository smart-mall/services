package ware.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.utils.PageUtils;
import common.utils.Query;
import common.utils.R;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ware.dao.WareSkuDao;
import ware.entity.WareSkuEntity;
import ware.feign.ProductFeignService;
import ware.service.WareSkuService;
import ware.vo.SkuHasStockVo;

import java.util.List;
import java.util.Map;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    private final ProductFeignService productFeignService;

    public WareSkuServiceImpl(ProductFeignService productFeignService) {
        this.productFeignService = productFeignService;
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        LambdaQueryWrapper<WareSkuEntity> queryWrapper = new LambdaQueryWrapper<>();

        String key = (String) params.get("skuId");
        if (key != null && !key.isEmpty()) {
            queryWrapper.eq(WareSkuEntity::getSkuId, key);
        }

        String wareId = (String) params.get("wareId");
        if (wareId != null && !wareId.isEmpty()) {
            queryWrapper.eq(WareSkuEntity::getWareId, wareId);
        }

        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                queryWrapper
        );

        return new PageUtils(page);
    }

    @Override
    @Transactional
    public void addStock(Long skuId, Long wareId, Integer skuNum) {
        WareSkuEntity wareSkuEntity = new WareSkuEntity();
        wareSkuEntity.setSkuId(skuId);
        wareSkuEntity.setWareId(wareId);
        wareSkuEntity.setStock(skuNum);
        wareSkuEntity.setStockLocked(0);
        try {
            R info = productFeignService.getProduct(skuId);

            @SuppressWarnings("unchecked")
            Map<String, Object> skuInfo =  (Map<String, Object>) info.get("skuInfo");

            if (info.getCode() == 0) {
                wareSkuEntity.setSkuName((String) skuInfo.get("skuName"));
            }
        } catch (Exception ignored) {}


        LambdaQueryWrapper<WareSkuEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WareSkuEntity::getSkuId, skuId)
                .eq(WareSkuEntity::getWareId, wareId);

        List<WareSkuEntity> wareSkuEntities = baseMapper.selectList(queryWrapper);
        if (wareSkuEntities == null || wareSkuEntities.isEmpty()) {
            baseMapper.insert(wareSkuEntity);
        } else {
            LambdaUpdateWrapper<WareSkuEntity> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(WareSkuEntity::getSkuId, skuId)
                    .eq(WareSkuEntity::getWareId, wareId)
                    .set(WareSkuEntity::getStock, wareSkuEntity.getStock() + skuNum);
            baseMapper.update(wareSkuEntity, updateWrapper);
        }

    }

    @Override
    public List<SkuHasStockVo> getSkusHasStock(List<Long> skuIds) {
        return skuIds.stream().map(skuId -> {
            SkuHasStockVo skuHasStockVo = new SkuHasStockVo();
            Long count = baseMapper.getSkuStock(skuId);
            skuHasStockVo.setSkuId(skuId);
            skuHasStockVo.setHasStock(count != null && count > 0);
            return skuHasStockVo;
        }).toList();
    }

}
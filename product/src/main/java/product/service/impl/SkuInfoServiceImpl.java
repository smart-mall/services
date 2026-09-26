package product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;
import common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import product.dao.SkuInfoDao;
import product.entity.SkuImagesEntity;
import product.entity.SkuInfoEntity;
import product.entity.SpuInfoDescEntity;
import product.feign.SeckillFeignService;
import product.feign.WareFeignService;
import product.service.*;
import product.vo.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;


import common.query.PageQuery;
import product.vo.SkuInfoPageQuery;
@Slf4j
@Service("skuInfoService")
public class SkuInfoServiceImpl extends ServiceImpl<SkuInfoDao, SkuInfoEntity> implements SkuInfoService {
    private final SpuInfoDescService spuInfoDescService;
    private final AttrGroupService attrGroupService;
    private final SkuSaleAttrValueService skuSaleAttrValueService;
    private final SeckillFeignService seckillFeignService;
    private final WareFeignService wareFeignService;
    private final ThreadPoolExecutor executor;
    private final SkuImagesService skuImagesService;

    public SkuInfoServiceImpl(SpuInfoDescService spuInfoDescService, AttrGroupService attrGroupService, SkuSaleAttrValueService skuSaleAttrValueService, SeckillFeignService seckillFeignService, WareFeignService wareFeignService, ThreadPoolExecutor executor, SkuImagesService skuImagesService) {
        this.spuInfoDescService = spuInfoDescService;
        this.attrGroupService = attrGroupService;
        this.skuSaleAttrValueService = skuSaleAttrValueService;
        this.seckillFeignService = seckillFeignService;
        this.wareFeignService = wareFeignService;
        this.executor = executor;
        this.skuImagesService = skuImagesService;
    }

    @Override
    public PageVO<SkuInfoEntity> queryPage(PageQuery query) {
        IPage<SkuInfoEntity> page = this.page(query.toPage());

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

    @Override
    public PageVO<SkuInfoEntity> queryPageByCondition(SkuInfoPageQuery query) {
        LambdaQueryWrapper<SkuInfoEntity> queryWrapper = new LambdaQueryWrapper<>();

        String key = query.getKey();
        if (key != null && !key.isEmpty()) {
            queryWrapper.and(wrapper -> wrapper.eq(SkuInfoEntity::getSkuId, key).or().like(SkuInfoEntity::getSkuName, key));
        }

        String catalogId = query.getCatalogId();
        if (catalogId != null && !catalogId.isEmpty() && !"0".equals(catalogId)) {
            queryWrapper.eq(SkuInfoEntity::getCatalogId, catalogId);
        }

        String brandId = query.getBrandId();
        if (brandId != null && !brandId.isEmpty() && !"0".equals(brandId)) {
            queryWrapper.eq(SkuInfoEntity::getBrandId, brandId);
        }

        Integer min = query.getMin();
        Integer max = query.getMax();
        if (min != null && max != null && min >= 0 && min < max) {
            queryWrapper.ge(SkuInfoEntity::getPrice, min);
            queryWrapper.le(SkuInfoEntity::getPrice, max);
        }

        IPage<SkuInfoEntity> page = this.page(
                query.toPage(),
                queryWrapper
        );

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

    @Override
    public SkuItemVo item(Long skuId) throws ExecutionException, InterruptedException {

        SkuItemVo skuItemVo = new SkuItemVo();

        CompletableFuture<SkuInfoEntity> infoFuture = CompletableFuture.supplyAsync(() -> {
            //1、sku基本信息的获取  pms_sku_info
            SkuInfoEntity info = this.getById(skuId);
            skuItemVo.setInfo(info);
            return info;
        }, executor);

        // 商品不存在时提前返回：下面的销售属性、介绍、规格参数都要用 info.getSpuId()，
        // 不拦的话会在异步线程里抛 NPE，.get() 再抛 ExecutionException，前端拿到的是一个 500
        if (infoFuture.join() == null) {
            return skuItemVo;
        }

        CompletableFuture<Void> saleAttrFuture = infoFuture.thenAcceptAsync((res) -> {
            //3、获取spu的销售属性组合
            List<SkuItemSaleAttrVo> saleAttrVos = skuSaleAttrValueService.getSaleAttrBySpuId(res.getSpuId());
            skuItemVo.setSaleAttr(saleAttrVos);
        }, executor);


        CompletableFuture<Void> descFuture = infoFuture.thenAcceptAsync((res) -> {
            //4、获取spu的介绍    pms_spu_info_desc
            SpuInfoDescEntity spuInfoDescEntity = spuInfoDescService.getById(res.getSpuId());
            skuItemVo.setDesc(spuInfoDescEntity);
        }, executor);


        CompletableFuture<Void> baseAttrFuture = infoFuture.thenAcceptAsync((res) -> {
            //5、获取spu的规格参数信息
            List<SpuItemAttrGroupVo> attrGroupVos = attrGroupService.getAttrGroupWithAttrsBySpuId(res.getSpuId(), res.getCatalogId());
            skuItemVo.setGroupAttrs(attrGroupVos);
        }, executor);


//        创建第二个异步任务
        //2、sku的图片信息    pms_sku_images
        CompletableFuture<Void> imageFuture = CompletableFuture.runAsync(() -> {
            List<SkuImagesEntity> imagesEntities = skuImagesService.getImagesBySkuId(skuId);
            skuItemVo.setImages(imagesEntities);
        }, executor);

        //3、远程调用查询当前sku是否参与秒杀优惠活动
        CompletableFuture<Void> seckillFuture = CompletableFuture.runAsync(() -> {
            R<SeckillSkuVo> skuSeckillInfo = seckillFeignService.getSkuSeckilInfo(skuId);
            if (skuSeckillInfo.getCode() == 0) {
                //查询成功
                SeckillSkuVo seckillInfoData = skuSeckillInfo.getData();
                skuItemVo.setSeckillSkuVo(seckillInfoData);

                if (seckillInfoData != null) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime > seckillInfoData.getEndTime()) {
                        skuItemVo.setSeckillSkuVo(null);
                    }
                }
            }
        }, executor);


        //4、远程调用库存服务，查询当前sku是否有货
        CompletableFuture<Void> stockFuture = CompletableFuture.runAsync(() -> {
            try {
                R<List<SkuHasStockVo>> skuStockInfo = wareFeignService.getSkusHasStock(List.of(skuId));
                if (skuStockInfo.getCode() == 0) {
                    List<SkuHasStockVo> skuHasStockVos = skuStockInfo.getData();
                    if (skuHasStockVos != null && !skuHasStockVos.isEmpty()
                            && skuHasStockVos.get(0).getHasStock() != null) {
                        skuItemVo.setHasStock(skuHasStockVos.get(0).getHasStock());
                    }
                }
            } catch (Exception e) {
                // 库存服务异常不能让商品详情页跟着挂，保持 SkuItemVo 里默认的"有货"
                log.error("查询库存失败，skuId={}", skuId, e);
            }
        }, executor);


        //等到所有任务都完成
        CompletableFuture
                .allOf(saleAttrFuture,descFuture,baseAttrFuture,imageFuture,seckillFuture,stockFuture)
                .get();

        return skuItemVo;
    }

    @Override
    public List<SkuSelectVO> getSkuSelect() {
        List<SkuInfoEntity> spuInfoEntities = baseMapper.selectList(null);
        return spuInfoEntities.stream().map(item -> {
            SkuSelectVO spuSelectVO = new SkuSelectVO();
            spuSelectVO.setId(item.getSkuId());
            spuSelectVO.setName(item.getSkuName());
            return spuSelectVO;
        }).toList();
    }

    @Override
    public Map<Long, String> getUserNames(List<Long> list) {
        List<SkuInfoEntity> spuInfoEntities = baseMapper.selectByIds(list);
        return spuInfoEntities.stream().collect(Collectors.toMap(SkuInfoEntity::getSkuId, SkuInfoEntity::getSkuName));
    }

    public List<SkuInfoEntity> getSkusBySpuId(Long spuId) {
        return this.list(new LambdaQueryWrapper<SkuInfoEntity>().eq(SkuInfoEntity::getSpuId, spuId));
    }
}
package product.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.utils.PageUtils;
import common.utils.Query;
import common.utils.R;
import org.springframework.stereotype.Service;
import product.dao.SkuInfoDao;
import product.entity.SkuImagesEntity;
import product.entity.SkuInfoEntity;
import product.entity.SpuInfoDescEntity;
import product.feign.SeckillFeignService;
import product.service.*;
import product.vo.SeckillSkuVo;
import product.vo.SkuItemSaleAttrVo;
import product.vo.SkuItemVo;
import product.vo.SpuItemAttrGroupVo;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;


@Service("skuInfoService")
public class SkuInfoServiceImpl extends ServiceImpl<SkuInfoDao, SkuInfoEntity> implements SkuInfoService {
    private final SpuInfoDescService spuInfoDescService;
    private final AttrGroupService attrGroupService;
    private final SkuSaleAttrValueService skuSaleAttrValueService;
    private final SeckillFeignService seckillFeignService;
    private final ThreadPoolExecutor executor;
    private final SkuImagesService skuImagesService;

    public SkuInfoServiceImpl(SpuInfoDescService spuInfoDescService, AttrGroupService attrGroupService, SkuSaleAttrValueService skuSaleAttrValueService, SeckillFeignService seckillFeignService, ThreadPoolExecutor executor, SkuImagesService skuImagesService) {
        this.spuInfoDescService = spuInfoDescService;
        this.attrGroupService = attrGroupService;
        this.skuSaleAttrValueService = skuSaleAttrValueService;
        this.seckillFeignService = seckillFeignService;
        this.executor = executor;
        this.skuImagesService = skuImagesService;
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                new QueryWrapper<>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        LambdaQueryWrapper<SkuInfoEntity> queryWrapper = new LambdaQueryWrapper<>();

        String key = (String) params.get("key");
        if (key != null && !key.isEmpty()) {
            queryWrapper.and(wrapper -> wrapper.eq(SkuInfoEntity::getSkuId, key).or().like(SkuInfoEntity::getSkuName, key));
        }

        String catalogId = (String) params.get("catalogId");
        if (catalogId != null && !catalogId.isEmpty() && !"0".equals(catalogId)) {
            queryWrapper.eq(SkuInfoEntity::getCatalogId, catalogId);
        }

        String brandId = (String) params.get("brandId");
        if (brandId != null && !brandId.isEmpty() && !"0".equals(brandId)) {
            queryWrapper.eq(SkuInfoEntity::getBrandId, brandId);
        }

        int min = Integer.parseInt((String) params.get("min"));
        int max = Integer.parseInt((String) params.get("max"));
        if (min >= 0 && min < max) {
            queryWrapper.ge(SkuInfoEntity::getPrice, min);
            queryWrapper.le(SkuInfoEntity::getPrice, max);
        }

        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                queryWrapper
        );

        return new PageUtils(page);
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
            R skuSeckillInfo = seckillFeignService.getSkuSeckilInfo(skuId);
            if (skuSeckillInfo.getCode() == 0) {
                //查询成功
                SeckillSkuVo seckillInfoData = skuSeckillInfo.getData("data", new TypeReference<>() {});
                skuItemVo.setSeckillSkuVo(seckillInfoData);

                if (seckillInfoData != null) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime > seckillInfoData.getEndTime()) {
                        skuItemVo.setSeckillSkuVo(null);
                    }
                }
            }
        }, executor);


        //等到所有任务都完成
        CompletableFuture
                .allOf(saleAttrFuture,descFuture,baseAttrFuture,imageFuture,seckillFuture)
                .get();

        return skuItemVo;
    }

    public List<SkuInfoEntity> getSkusBySpuId(Long spuId) {
        return this.list(new LambdaQueryWrapper<SkuInfoEntity>().eq(SkuInfoEntity::getSpuId, spuId));
    }
}
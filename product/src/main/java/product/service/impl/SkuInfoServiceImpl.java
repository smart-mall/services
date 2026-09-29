package product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.to.SkuScopeVo;
import common.vo.PageVO;
import common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import product.dao.SkuInfoDao;
import product.dao.SpuInfoDao;
import product.entity.SkuImagesEntity;
import product.entity.SkuInfoEntity;
import product.entity.SpuInfoDescEntity;
import product.entity.SpuInfoEntity;
import product.feign.SeckillFeignService;
import product.feign.WareFeignService;
import product.service.*;
import product.vo.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;


import common.query.PageQuery;
import product.vo.SkuInfoPageQuery;
/**
 * sku 服务的默认实现，基于 MyBatis-Plus 的 {@code ServiceImpl} 读写 {@code pms_sku_info}。
 *
 * <p>商品详情页的数据组装走构造器注入的线程池：本地查询与秒杀、库存两个远程调用并行；库存服务
 * 异常只记日志，按默认的「有货」返回。
 */
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
    private final SpuInfoDao spuInfoDao;

    /**
     * 由容器注入详情页各数据源、两个远程客户端与线程池构造。
     *
     * @param spuInfoDescService spu 介绍服务，取商品描述图
     * @param attrGroupService 属性分组服务，取规格参数分组及取值
     * @param skuSaleAttrValueService sku 销售属性值服务，取销售属性组合
     * @param seckillFeignService 秒杀服务客户端，查当前 sku 是否参与秒杀
     * @param wareFeignService 库存服务客户端，查当前 sku 是否有货
     * @param executor 详情页并行加载各数据块用的线程池
     * @param skuImagesService sku 图片服务，取 sku 图集
     * @param spuInfoDao spu 主表数据访问，按 spu 批量取所属分类
     */
    public SkuInfoServiceImpl(SpuInfoDescService spuInfoDescService, AttrGroupService attrGroupService, SkuSaleAttrValueService skuSaleAttrValueService, SeckillFeignService seckillFeignService, WareFeignService wareFeignService, ThreadPoolExecutor executor, SkuImagesService skuImagesService, SpuInfoDao spuInfoDao) {
        this.spuInfoDescService = spuInfoDescService;
        this.attrGroupService = attrGroupService;
        this.skuSaleAttrValueService = skuSaleAttrValueService;
        this.seckillFeignService = seckillFeignService;
        this.wareFeignService = wareFeignService;
        this.executor = executor;
        this.skuImagesService = skuImagesService;
        this.spuInfoDao = spuInfoDao;
    }

    /** {@inheritDoc} */
    @Override
    public PageVO<SkuInfoEntity> queryPage(PageQuery query) {
        IPage<SkuInfoEntity> page = this.page(query.toPage());

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

    /** {@inheritDoc} */
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

    /**
     * {@inheritDoc}
     *
     * <p>只有 sku 基本信息是串行的：主线程 {@code join} 等它返回，拿到 {@code spuId} 才能发起
     * 后面的查询，商品不存在时也靠它提前返回。其余六段在线程池里并行 —— 图集、秒杀、库存三段
     * 不依赖基本信息；销售属性、商品介绍、规格参数三段挂在基本信息完成后执行。
     */
    @Override
    public SkuItemVo item(Long skuId) throws ExecutionException, InterruptedException {

        SkuItemVo skuItemVo = new SkuItemVo();

        CompletableFuture<SkuInfoEntity> infoFuture = CompletableFuture.supplyAsync(() -> {
            // 1. sku 基本信息：主线程要等它返回，后面的查询都要用它的 spuId
            SkuInfoEntity info = this.getById(skuId);
            skuItemVo.setInfo(info);
            return info;
        }, executor);

        // 商品不存在时提前返回：下面的销售属性、介绍、规格参数都要用 info.getSpuId()，
        // 不拦的话会在异步线程里抛 NPE，.get() 再抛 ExecutionException，前端拿到的是一个 500
        if (infoFuture.join() == null) {
            return skuItemVo;
        }

        // 2. 依赖 spuId 的三段，在基本信息完成后并行：销售属性、商品介绍、规格参数
        CompletableFuture<Void> saleAttrFuture = infoFuture.thenAcceptAsync((res) -> {
            List<SkuItemSaleAttrVo> saleAttrVos = skuSaleAttrValueService.getSaleAttrBySpuId(res.getSpuId());
            skuItemVo.setSaleAttr(saleAttrVos);
        }, executor);


        CompletableFuture<Void> descFuture = infoFuture.thenAcceptAsync((res) -> {
            SpuInfoDescEntity spuInfoDescEntity = spuInfoDescService.getById(res.getSpuId());
            skuItemVo.setDesc(spuInfoDescEntity);
        }, executor);


        CompletableFuture<Void> baseAttrFuture = infoFuture.thenAcceptAsync((res) -> {
            List<SpuItemAttrGroupVo> attrGroupVos = attrGroupService.getAttrGroupWithAttrsBySpuId(res.getSpuId(), res.getCatalogId());
            skuItemVo.setGroupAttrs(attrGroupVos);
        }, executor);


        // 3. 不依赖基本信息的三段，与上面并行：sku 图集、秒杀、库存
        CompletableFuture<Void> imageFuture = CompletableFuture.runAsync(() -> {
            List<SkuImagesEntity> imagesEntities = skuImagesService.getImagesBySkuId(skuId);
            skuItemVo.setImages(imagesEntities);
        }, executor);

        CompletableFuture<Void> seckillFuture = CompletableFuture.runAsync(() -> {
            R<SeckillSkuVo> skuSeckillInfo = seckillFeignService.getSkuSeckilInfo(skuId);
            if (skuSeckillInfo.getCode() == 0) {
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


        // 4. 等全部任务结束；任一段抛出的异常都在这里以 ExecutionException 冒给调用方
        CompletableFuture
                .allOf(saleAttrFuture,descFuture,baseAttrFuture,imageFuture,seckillFuture,stockFuture)
                .get();

        return skuItemVo;
    }

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
    @Override
    public Map<Long, String> getUserNames(List<Long> list) {
        // 必须先判空：空集合会拼出 IN ()，MySQL 报语法错，整页接口变成 10000
        if (list == null || list.isEmpty()) {
            return Map.of();
        }
        List<SkuInfoEntity> spuInfoEntities = baseMapper.selectByIds(list);
        return spuInfoEntities.stream()
                .filter(item -> item.getSkuName() != null)
                .collect(Collectors.toMap(SkuInfoEntity::getSkuId, SkuInfoEntity::getSkuName, (first, second) -> first));
    }

    /** {@inheritDoc} */
    @Override
    public Map<Long, SkuScopeVo> getSkuScopes(List<Long> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return Map.of();
        }

        List<SkuInfoEntity> skus = baseMapper.selectByIds(skuIds);
        if (skus.isEmpty()) {
            return Map.of();
        }

        // 两级归属分两次按主键批量查，不做 join：sku 主表给 spu，spu 主表给分类
        List<Long> spuIds = skus.stream()
                .map(SkuInfoEntity::getSpuId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        // 注入 dao 而非 SpuInfoService：后者构造器反过来依赖本类，
        // 两边都用构造器注入会形成循环，容器启动时直接失败
        Map<Long, Long> catalogBySpuId = spuIds.isEmpty() ? Map.of()
                : spuInfoDao.selectByIds(spuIds).stream()
                        .filter(spu -> spu.getCatalogId() != null)
                        .collect(Collectors.toMap(SpuInfoEntity::getId, SpuInfoEntity::getCatalogId, (first, second) -> first));

        Map<Long, SkuScopeVo> scopes = new HashMap<>();
        for (SkuInfoEntity sku : skus) {
            SkuScopeVo scope = new SkuScopeVo();
            scope.setSkuId(sku.getSkuId());
            scope.setSpuId(sku.getSpuId());
            scope.setCatalogId(catalogBySpuId.get(sku.getSpuId()));
            scopes.put(sku.getSkuId(), scope);
        }
        return scopes;
    }

    /**
     * 查询某个 spu 下的全部 sku，供商品上架时组装索引文档。
     *
     * @param spuId spu ID，不能为 {@code null}
     * @return 该 spu 的 sku 列表；没有 sku 时返回空列表
     */
    public List<SkuInfoEntity> getSkusBySpuId(Long spuId) {
        return this.list(new LambdaQueryWrapper<SkuInfoEntity>().eq(SkuInfoEntity::getSpuId, spuId));
    }
}
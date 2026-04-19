package product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import common.to.SkuReductionTo;
import common.to.SpuBoundTo;
import es.SkuEsModel;
import common.utils.PageUtils;
import common.utils.Query;
import common.utils.R;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import product.constant.ProductStatusEnum;
import product.dao.*;
import product.entity.*;
import product.feign.CouponFeignService;
import product.feign.SearchFeignService;
import product.feign.WareFeignService;
import product.service.*;
import product.vo.SkuHasStockVo;
import product.vo.SpuVO;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;


@Service("spuInfoService")
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {

    private final SpuInfoDescDao spuInfoDescDao;
    private final SpuInfoDao spuInfoDao;
    private final SpuImagesDao spuImagesDao;
    private final ProductAttrValueDao productAttrValueDao;
    private final ProductAttrValueService productAttrValueService;
    private final AttrDao attrDao;
    private final SkuInfoDao skuInfoDao;
    private final SkuImagesDao skuImagesDao;
    private final SkuSaleAttrValueDao skuSaleAttrValueDao;
    private final CouponFeignService couponFeignService;
    private final SkuInfoServiceImpl skuInfoService;
    private final BrandService brandService;
    private final CategoryService categoryService;
    private final AttrService attrService;
    private final WareFeignService wareFeignService;
    private final SearchFeignService searchFeignService;
    private final ObjectMapper objectMapper;

    public SpuInfoServiceImpl(SpuInfoDao spuInfoDao, SpuInfoDescDao spuInfoDescDao, SpuImagesDao spuImagesDao, ProductAttrValueDao productAttrValueDao, ProductAttrValueService productAttrValueService, AttrDao attrDao, SkuInfoDao skuInfoDao, SkuImagesDao skuImagesDao, SkuSaleAttrValueDao skuSaleAttrValueDao, CouponFeignService couponFeignService, SkuInfoServiceImpl skuInfoService, BrandService brandService, CategoryService categoryService, AttrService attrService, WareFeignService wareFeignService, SearchFeignService searchFeignService, ObjectMapper objectMapper) {
        this.spuInfoDao = spuInfoDao;
        this.spuInfoDescDao = spuInfoDescDao;
        this.spuImagesDao = spuImagesDao;
        this.productAttrValueDao = productAttrValueDao;
        this.productAttrValueService = productAttrValueService;
        this.attrDao = attrDao;
        this.skuInfoDao = skuInfoDao;
        this.skuImagesDao = skuImagesDao;
        this.skuSaleAttrValueDao = skuSaleAttrValueDao;
        this.couponFeignService = couponFeignService;
        this.skuInfoService = skuInfoService;
        this.brandService = brandService;
        this.categoryService = categoryService;
        this.attrService = attrService;
        this.wareFeignService = wareFeignService;
        this.searchFeignService = searchFeignService;
        this.objectMapper = objectMapper;
    }


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                new QueryWrapper<>()
        );

        return new PageUtils(page);
    }

    @Override
    @Transactional
    public void saveSpuInfo(SpuVO spuInfo) {
        // spu基本信息
        SpuInfoEntity spuInfoEntity = new SpuInfoEntity();
        BeanUtils.copyProperties(spuInfo, spuInfoEntity);

        Date now = new Date();
        spuInfoEntity.setCreateTime(now);
        spuInfoEntity.setUpdateTime(now);

        spuInfoDao.insert(spuInfoEntity);


        // 描述图片
        List<String> discrip = spuInfo.getDecript();
        if (discrip != null && !discrip.isEmpty()) {
            SpuInfoDescEntity spuInfoDescEntity = new SpuInfoDescEntity();
            spuInfoDescEntity.setSpuId(spuInfoEntity.getId());
            spuInfoDescEntity.setDecript(String.join(",", discrip));

            spuInfoDescDao.insert(spuInfoDescEntity);
        }


        // 图片集
        List<String> images = spuInfo.getImages();
        if (images != null && !images.isEmpty()) {
            Long id = spuInfoEntity.getId();
            List<SpuImagesEntity> spuImagesEntityStream = images.stream().map(item -> {
                SpuImagesEntity spuImagesEntity = new SpuImagesEntity();
                spuImagesEntity.setSpuId(id);
                spuImagesEntity.setImgUrl(item);

                return spuImagesEntity;
            }).toList();

            spuImagesDao.insert(spuImagesEntityStream);
        }


        // 积分信息
        SpuBoundTo spuBoundTo = new SpuBoundTo();

        spuBoundTo.setSpuId(spuInfoEntity.getId());
        BeanUtils.copyProperties(spuInfo.getBounds(), spuBoundTo);
        R r1 = couponFeignService.saveSpuBounds(spuBoundTo);
        if (r1.getCode() != 0) {
            throw new RuntimeException("保存积分信息失败");
        }

        // 规格参数
        List<SpuVO.BaseAttrs> baseAttrs = spuInfo.getBaseAttrs();
        if (baseAttrs != null && !baseAttrs.isEmpty()) {
            Long id = spuInfoEntity.getId();
            List<ProductAttrValueEntity> spuInfoEntityStream = baseAttrs.stream().map(item -> {
                ProductAttrValueEntity productAttrValueEntity = new ProductAttrValueEntity();
                productAttrValueEntity.setSpuId(id);
                AttrEntity attrEntity = attrDao.selectById(item.getAttrId());
                productAttrValueEntity.setAttrName(attrEntity.getAttrName());
                productAttrValueEntity.setAttrValue(item.getAttrValues());
                productAttrValueEntity.setAttrId(item.getAttrId());
                productAttrValueEntity.setAttrSort(item.getShowDesc());

                return productAttrValueEntity;
            }).toList();

            productAttrValueDao.insert(spuInfoEntityStream);
        }

        // sku 信息
        List<SpuVO.Sku> skus = spuInfo.getSkus();
        if (skus != null && !skus.isEmpty()) {
            skus.forEach(sku -> {
                // sku 基本信息
                String defaultImage = "";
                for (SpuVO.Images image : sku.getImages()) {
                    if (image.getDefaultImg() == 1) {
                        defaultImage = image.getImgUrl();
                    }
                }

                SkuInfoEntity skuInfoEntity = new SkuInfoEntity();
                BeanUtils.copyProperties(sku, skuInfoEntity);
                skuInfoEntity.setBrandId(spuInfoEntity.getBrandId());
                skuInfoEntity.setCatelogId(spuInfoEntity.getCatelogId());
                skuInfoEntity.setSaleCount(0L);
                skuInfoEntity.setSkuDefaultImg(defaultImage);
                skuInfoEntity.setSpuId(spuInfoEntity.getId());

                skuInfoDao.insert(skuInfoEntity);

                // sku 图片信息
                Long skuId = skuInfoEntity.getSkuId();

                List<SkuImagesEntity> skuImagesEntityStream = sku.getImages().stream().map(item -> {
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                    skuImagesEntity.setSkuId(skuId);
                    skuImagesEntity.setImgUrl(item.getImgUrl());
                    skuImagesEntity.setImgSort(item.getDefaultImg());

                    return skuImagesEntity;
                }).filter( item -> !item.getImgUrl().isEmpty()).toList();

                skuImagesDao.insert(skuImagesEntityStream);

                // sku 销售信息
                List<SpuVO.Attr> attr = sku.getAttr();
                List<SkuSaleAttrValueEntity> skuSaleAttrValueEntityStream = attr.stream().map(item -> {
                    SkuSaleAttrValueEntity skuSaleAttrValueEntity = new SkuSaleAttrValueEntity();
                    BeanUtils.copyProperties(item, skuSaleAttrValueEntity);
                    skuSaleAttrValueEntity.setSkuId(skuId);
                    return skuSaleAttrValueEntity;
                }).toList();
                skuSaleAttrValueDao.insert(skuSaleAttrValueEntityStream);

                // sku 优惠信息
                SkuReductionTo skuReductionTo = new SkuReductionTo();
                BeanUtils.copyProperties(sku, skuReductionTo);
                skuReductionTo.setSkuId(skuInfoEntity.getSkuId());

                if (skuReductionTo.getFullCount() > 0 || skuReductionTo.getFullPrice().compareTo(new BigDecimal("0")) > 0) {
                    R r = couponFeignService.saveSkuReduction(skuReductionTo);
                    if (r.getCode() != 0) {
                        log.error("保存sku优惠信息失败");
                    }
                }
            });

        }


    }

    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        LambdaQueryWrapper<SpuInfoEntity> queryWrapper = new LambdaQueryWrapper<>();

        String key = (String) params.get("key");
        if (key != null && !key.isEmpty()) {
            queryWrapper.and(item -> item.eq(SpuInfoEntity::getId, key).or().like(SpuInfoEntity::getSpuName, key));
        }

        String status = (String) params.get("status");
        if (status != null && !status.isEmpty()) {
            queryWrapper.eq(SpuInfoEntity::getPublishStatus, status);
        }

        String brandId = (String) params.get("brandId");
        if (brandId != null && !brandId.isEmpty() && !"0".equals(brandId)) {
            queryWrapper.eq(SpuInfoEntity::getBrandId, brandId);
        }

        String catelogId = (String) params.get("catelogId");
        if (catelogId != null && !catelogId.isEmpty()  && !"0".equals(catelogId)) {
            queryWrapper.eq(SpuInfoEntity::getCatelogId, catelogId);
        }


        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                queryWrapper
        );

        return new PageUtils(page);
    }

    @Override
    public void up(Long spuId) {
        List<SkuInfoEntity> skus = skuInfoService.getSkusBySpuId(spuId);

        List<ProductAttrValueEntity> baseAttrs = productAttrValueService.baseAttrListForSpu(spuId);
        List<Long> attrIds = baseAttrs.stream().map(ProductAttrValueEntity::getAttrId).toList();

        List<Long> searchAttrIds = attrService.selectSearchAttrs(attrIds);

        Set<Long> idSet = new HashSet<>(searchAttrIds);
        List<SkuEsModel.Attrs> attrsList = baseAttrs.stream()
                .filter(item -> idSet.contains(item.getAttrId()))
                .map(item -> {
                    SkuEsModel.Attrs attrs01 = new SkuEsModel.Attrs();
                    BeanUtils.copyProperties(item, attrs01);
                    return attrs01;
                })
                .toList();

        List<Long> skuIdList = skus.stream().map(SkuInfoEntity::getSkuId).toList();
        Map<Long, Boolean> stockMap = null;
        try {
            R res = wareFeignService.getSkusHasStock(skuIdList);
            Object o = res.get("data");
            List<SkuHasStockVo> skuHasStockVos = new ArrayList<>();
            if (o instanceof List) {
                skuHasStockVos = objectMapper.convertValue(o,
                        new TypeReference<>() {
                        });
            }
            stockMap = skuHasStockVos.stream().collect(Collectors.toMap(SkuHasStockVo::getSkuId, SkuHasStockVo::getHasStock));
        } catch (Exception e) {
            log.error("库存服务异常", e);
        }


        Map<Long, Boolean> finalStockMap = stockMap;
        List<SkuEsModel> list = skus.stream().map(item -> {
            SkuEsModel model = new SkuEsModel();
            BeanUtils.copyProperties(item, model);

            model.setSkuPrice(item.getPrice());
            model.setSkuImg(item.getSkuDefaultImg());

            if (finalStockMap == null) {
                model.setHasStock(true);
            } else {
                model.setHasStock(finalStockMap.get(item.getSkuId()));
            }

            model.setHotScore(0L);

            BrandEntity brand = brandService.getById(item.getBrandId());
            model.setBrandName(brand.getName());
            model.setBrandImg(brand.getLogo());

            CategoryEntity byId = categoryService.getById(item.getCatelogId());
            model.setCatalogName(byId.getName());

            model.setAttrs(attrsList);

            return model;
        }).toList();

        R r = searchFeignService.productStatusUp(list);
        if (r.getCode() == 0) {
            this.baseMapper.updateSpuStatus(spuId, ProductStatusEnum.UP.getCode());
        }
    }


}
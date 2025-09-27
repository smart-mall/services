package product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.to.SkuReductionTo;
import common.to.SpuBoundTo;
import common.utils.PageUtils;
import common.utils.Query;
import common.utils.R;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import product.dao.*;
import product.entity.*;
import product.feign.CouponFeignService;
import product.service.SpuInfoService;
import product.vo.SpuVO;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;


@Service("spuInfoService")
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {

    private final SpuInfoDescDao spuInfoDescDao;
    private final SpuInfoDao spuInfoDao;
    private final SpuImagesDao spuImagesDao;
    private final ProductAttrValueDao productAttrValueDao;
    private final AttrDao attrDao;
    private final SkuInfoDao skuInfoDao;
    private final SkuImagesDao skuImagesDao;
    private final SkuSaleAttrValueDao skuSaleAttrValueDao;
    private final CouponFeignService couponFeignService;

    public SpuInfoServiceImpl(SpuInfoDao spuInfoDao, SpuInfoDescDao spuInfoDescDao, SpuImagesDao spuImagesDao, ProductAttrValueDao productAttrValueDao, AttrDao attrDao, SkuInfoDao skuInfoDao, SkuImagesDao skuImagesDao, SkuSaleAttrValueDao skuSaleAttrValueDao, CouponFeignService couponFeignService) {
        this.spuInfoDao = spuInfoDao;
        this.spuInfoDescDao = spuInfoDescDao;
        this.spuImagesDao = spuImagesDao;
        this.productAttrValueDao = productAttrValueDao;
        this.attrDao = attrDao;
        this.skuInfoDao = skuInfoDao;
        this.skuImagesDao = skuImagesDao;
        this.skuSaleAttrValueDao = skuSaleAttrValueDao;
        this.couponFeignService = couponFeignService;
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
                skuInfoEntity.setCatalogId(spuInfoEntity.getCatalogId());
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


}
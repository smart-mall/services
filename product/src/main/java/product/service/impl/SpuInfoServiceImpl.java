package product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.constant.ProductConstant;
import common.exception.BaseCodeEnum;
import common.exception.BaseException;
import common.exception.ValidationException;
import common.mq.MqConstant;
import common.mq.outbox.ReliableMqPublisher;
import common.to.SkuDeleteBlockerTo;
import common.to.SkuReductionTo;
import common.to.SpuBoundTo;
import common.to.mq.ProductDeletedTo;
import common.to.mq.ProductDownTo;
import es.SkuEsModel;
import lombok.extern.slf4j.Slf4j;
import common.vo.PageVO;
import common.utils.R;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import product.dao.*;
import product.entity.*;
import product.feign.CouponFeignService;
import product.feign.SearchFeignService;
import product.feign.WareFeignService;
import product.service.*;
import product.vo.SkuHasStockVo;
import product.vo.SpuSelectVO;
import product.vo.SpuVO;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;


import product.entity.SpuInfoEntity;
import common.query.PageQuery;
import product.vo.SpuInfoPageQuery;
@Service("spuInfoService")
@Slf4j
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {

    /** 已上架商品的错误信息里最多列几个商品名，列多了前端 toast 显示不下 */
    private static final int MAX_UP_SHELVED_IN_MESSAGE = 3;

    /** 仓库阻塞信息里最多列几个 sku，同上 */
    private static final int MAX_WARE_BLOCKERS_IN_MESSAGE = 3;

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
    private final ReliableMqPublisher reliableMqPublisher;

    public SpuInfoServiceImpl(SpuInfoDao spuInfoDao, SpuInfoDescDao spuInfoDescDao, SpuImagesDao spuImagesDao, ProductAttrValueDao productAttrValueDao, ProductAttrValueService productAttrValueService, AttrDao attrDao, SkuInfoDao skuInfoDao, SkuImagesDao skuImagesDao, SkuSaleAttrValueDao skuSaleAttrValueDao, CouponFeignService couponFeignService, SkuInfoServiceImpl skuInfoService, BrandService brandService, CategoryService categoryService, AttrService attrService, WareFeignService wareFeignService, SearchFeignService searchFeignService, ReliableMqPublisher reliableMqPublisher) {
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
        this.reliableMqPublisher = reliableMqPublisher;
    }


    @Override
    public PageVO<SpuInfoEntity> queryPage(PageQuery query) {
        IPage<SpuInfoEntity> page = this.page(
                query.toPage(),
                new QueryWrapper<>()
        );

        return new PageVO<>(page.getTotal(), page.getRecords());
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
        R<Void> r1 = couponFeignService.saveSpuBounds(spuBoundTo);
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
                    R<Void> r = couponFeignService.saveSkuReduction(skuReductionTo);
                    if (r.getCode() != 0) {
                        log.error("保存sku优惠信息失败");
                    }
                }
            });

        }


    }

    @Override
    @Transactional
    public void removeSpuInfo(List<Long> spuIds) {
        List<Long> distinctSpuIds = spuIds == null
                ? List.of()
                : spuIds.stream().filter(Objects::nonNull).distinct().toList();
        if (distinctSpuIds.isEmpty()) {
            throw new ValidationException("ids", "请选择要删除的商品");
        }

        List<SpuInfoEntity> spus = this.listByIds(distinctSpuIds);

        // 上架中的商品不许删：它的 sku 还在 ES 里挂着，删了库搜索结果里就会留下
        // 点进去打不开的商品。
        // 批量删除要么全删要么全不删 —— 只删一半的话，调用方拿到的"成功"里混着没删掉的
        // id，还得自己去比对哪些成功了。
        List<SpuInfoEntity> upShelved = spus.stream()
                .filter(spu -> Objects.equals(spu.getPublishStatus(),
                        ProductConstant.ProductStatusEnum.UP.getCode()))
                .toList();
        if (!upShelved.isEmpty()) {
            throw new BaseException(BaseCodeEnum.PRODUCT_UP_SHELVED_CANNOT_DELETE,
                    describeUpShelved(upShelved));
        }

        // 查不到的 id 静默跳过：重复提交、id 传错都不该报错，删除本身是幂等的
        List<Long> existingSpuIds = spus.stream().map(SpuInfoEntity::getId).toList();
        if (existingSpuIds.isEmpty()) {
            return;
        }

        List<SkuInfoEntity> skus = skuInfoDao.selectList(new LambdaQueryWrapper<SkuInfoEntity>()
                .in(SkuInfoEntity::getSpuId, existingSpuIds));
        List<Long> skuIds = skus.stream().map(SkuInfoEntity::getSkuId).toList();

        // 仓库侧还有库存或没走完的采购需求就不许删：那是真实的货和在途单据，
        // 商品先没了它们就成无主数据
        assertWareCanDelete(skuIds);

        // 这些地址指向 MinIO 里的对象，必须在删行之前收集 —— 行一删就再也找不到它们了。
        List<String> imageUrls = collectImageUrls(existingSpuIds, skus);

        // 先删子表再删主表。库里没有外键，顺序其实不影响最终结果，
        // 但反过来会出现"主表已经没了、子表还在"的中间状态，看着就是脏数据。
        if (!skuIds.isEmpty()) {
            skuImagesDao.delete(new LambdaQueryWrapper<SkuImagesEntity>()
                    .in(SkuImagesEntity::getSkuId, skuIds));
            skuSaleAttrValueDao.delete(new LambdaQueryWrapper<SkuSaleAttrValueEntity>()
                    .in(SkuSaleAttrValueEntity::getSkuId, skuIds));
            skuInfoDao.delete(new LambdaQueryWrapper<SkuInfoEntity>()
                    .in(SkuInfoEntity::getSkuId, skuIds));
        }

        spuInfoDescDao.delete(new LambdaQueryWrapper<SpuInfoDescEntity>()
                .in(SpuInfoDescEntity::getSpuId, existingSpuIds));
        spuImagesDao.delete(new LambdaQueryWrapper<SpuImagesEntity>()
                .in(SpuImagesEntity::getSpuId, existingSpuIds));
        productAttrValueDao.delete(new LambdaQueryWrapper<ProductAttrValueEntity>()
                .in(ProductAttrValueEntity::getSpuId, existingSpuIds));
        this.removeByIds(existingSpuIds);

        // 跨服务的清理（coupon 的优惠数据、MinIO 图片）落一条本地消息，与上面 7 张表的删除同事务，
        // 提交后才投出去。远程删除不可回滚，不能直接调
        reliableMqPublisher.publish(
                MqConstant.Exchanges.PRODUCT_EVENT,
                MqConstant.RoutingKeys.PRODUCT_DELETED,
                new ProductDeletedTo(existingSpuIds, skuIds, imageUrls));

        log.info("级联删除商品完成：spuIds=" + existingSpuIds + "，skuIds=" + skuIds);
    }

    /**
     * 问仓库这些 sku 能不能删。
     *
     * <p>读不到 ware 时按"不能删"处理：放过去可能删掉还有库存的商品，那是不可逆的；
     * 拦下来最坏只是暂时删不掉。所以用独立的错误码，不和"确实有引用"混成一个 ——
     * 混了用户会去清数据，而实际上什么都不用清。</p>
     */
    private void assertWareCanDelete(List<Long> skuIds) {
        if (skuIds.isEmpty()) {
            return;
        }

        List<SkuDeleteBlockerTo> blockers = new ArrayList<>();
        try {
            R<List<SkuDeleteBlockerTo>> r = wareFeignService.canDelete(skuIds);
            if (r == null || r.getCode() != 0) {
                throw new IllegalStateException("ware 返回了非成功响应：" + (r == null ? "null" : r.getMsg()));
            }
            if (r.getData() != null) {
                blockers = r.getData();
            }
        } catch (Exception e) {
            log.error("询问仓库能否删除商品失败，按不能删处理：skuIds={}", skuIds, e);
            throw new BaseException(BaseCodeEnum.PRODUCT_WARE_UNAVAILABLE);
        }

        if (blockers.isEmpty()) {
            return;
        }
        throw new BaseException(BaseCodeEnum.PRODUCT_WARE_IN_USE, describeWareBlockers(blockers));
    }

    /** 把仓库返回的阻塞清单拼成一句人能读的话。列多了前端 toast 显示不下，所以截断 */
    private static String describeWareBlockers(List<SkuDeleteBlockerTo> blockers) {
        List<String> parts = new ArrayList<>();
        for (SkuDeleteBlockerTo blocker : blockers.stream().limit(MAX_WARE_BLOCKERS_IN_MESSAGE).toList()) {
            StringBuilder part = new StringBuilder("sku " + blocker.getSkuId());

            List<SkuDeleteBlockerTo.StockBlocker> stockBlockers = blocker.getStockBlockers();
            boolean hasStock = stockBlockers != null && !stockBlockers.isEmpty();
            if (hasStock) {
                part.append("：").append(stockBlockers.stream().map(stock -> {
                    String ware = StringUtils.hasText(stock.getWareName())
                            ? stock.getWareName() : ("仓库" + stock.getWareId());
                    return ware + " 还有 " + stock.getStock() + " 件（其中锁定 " + stock.getStockLocked() + " 件）";
                }).collect(Collectors.joining("、")));
            }

            Integer purchaseCount = blocker.getPurchaseBlockerCount();
            if (purchaseCount != null && purchaseCount > 0) {
                part.append(hasStock ? "，" : "：")
                        .append("有 ").append(purchaseCount).append(" 条未完成的采购需求");
            }
            parts.add(part.toString());
        }

        String detail = String.join("；", parts);
        if (blockers.size() > MAX_WARE_BLOCKERS_IN_MESSAGE) {
            detail = detail + " 等 " + blockers.size() + " 个 sku";
        }
        return "商品在仓库还有库存或未完成的采购需求（" + detail + "），请先处理后再删除";
    }

    /**
     * 收集这个商品引用到的所有文件地址：spu 图集、sku 图集、sku 默认图、商品描述图。
     *
     * <p>sku 默认图正常情况下是 sku 图集里 defaultImg=1 的那张（见 {@code saveSpuInfo}），
     * 和上面重复，靠 {@code distinct} 去重；但它终究是独立的一列，万一被单独改过也不能漏，
     * 所以照样收集。</p>
     */
    private List<String> collectImageUrls(List<Long> spuIds, List<SkuInfoEntity> skus) {
        List<String> urls = new ArrayList<>();

        spuImagesDao.selectList(new LambdaQueryWrapper<SpuImagesEntity>()
                        .in(SpuImagesEntity::getSpuId, spuIds))
                .forEach(image -> urls.add(image.getImgUrl()));

        List<Long> skuIds = skus.stream().map(SkuInfoEntity::getSkuId).toList();
        if (!skuIds.isEmpty()) {
            skuImagesDao.selectList(new LambdaQueryWrapper<SkuImagesEntity>()
                            .in(SkuImagesEntity::getSkuId, skuIds))
                    .forEach(image -> urls.add(image.getImgUrl()));
            skus.forEach(sku -> urls.add(sku.getSkuDefaultImg()));
        }

        // 商品描述存的是逗号拼接的一组地址，见 saveSpuInfo 里的 String.join(",", decript)
        spuInfoDescDao.selectList(new LambdaQueryWrapper<SpuInfoDescEntity>()
                        .in(SpuInfoDescEntity::getSpuId, spuIds))
                .forEach(desc -> urls.addAll(splitDecript(desc.getDecript())));

        return urls.stream().filter(StringUtils::hasText).distinct().toList();
    }

    private static List<String> splitDecript(String decript) {
        if (!StringUtils.hasText(decript)) {
            return List.of();
        }
        return Arrays.stream(decript.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    /**
     * 批量删除时只回一句"有商品已上架"，调用方还得自己猜是哪一个。这里把名字带上，
     * 数量多时截断，省得 msg 长到前端 toast 显示不下。
     */
    private static String describeUpShelved(List<SpuInfoEntity> upShelved) {
        String names = upShelved.stream()
                .limit(MAX_UP_SHELVED_IN_MESSAGE)
                .map(spu -> spu.getSpuName() != null ? spu.getSpuName() : ("id=" + spu.getId()))
                .collect(Collectors.joining("、"));
        if (upShelved.size() > MAX_UP_SHELVED_IN_MESSAGE) {
            names = names + " 等 " + upShelved.size() + " 个商品";
        }
        return "商品「" + names + "」已上架，请先下架再删除";
    }

    @Override
    public PageVO<SpuInfoEntity> queryPageByCondition(SpuInfoPageQuery query) {
        LambdaQueryWrapper<SpuInfoEntity> queryWrapper = new LambdaQueryWrapper<>();

        String key = query.getKey();
        if (key != null && !key.isEmpty()) {
            queryWrapper.and(item -> item.eq(SpuInfoEntity::getId, key).or().like(SpuInfoEntity::getSpuName, key));
        }

        String status = query.getStatus();
        if (status != null && !status.isEmpty()) {
            queryWrapper.eq(SpuInfoEntity::getPublishStatus, status);
        }

        String brandId = query.getBrandId();
        if (brandId != null && !brandId.isEmpty() && !"0".equals(brandId)) {
            queryWrapper.eq(SpuInfoEntity::getBrandId, brandId);
        }

        String catalogId = query.getCatalogId();
        if (catalogId != null && !catalogId.isEmpty()  && !"0".equals(catalogId)) {
            queryWrapper.eq(SpuInfoEntity::getCatalogId, catalogId);
        }


        IPage<SpuInfoEntity> page = this.page(
                query.toPage(),
                queryWrapper
        );

        Map<Long, String> brandName = brandService.listByIds(page.getRecords().stream().map(SpuInfoEntity::getBrandId).toList())
                .stream().collect(Collectors.toMap(BrandEntity::getBrandId, BrandEntity::getName));

        Map<Long, String> catalogName = categoryService.listByIds(page.getRecords().stream().map(SpuInfoEntity::getCatalogId).toList())
                .stream().collect(Collectors.toMap(CategoryEntity::getCatId, CategoryEntity::getName));

        page.getRecords().forEach(item -> {
            item.setBrandName(brandName.get(item.getBrandId()));
            item.setCatalogName(catalogName.get(item.getCatalogId()));
        });


        return new PageVO<>(page.getTotal(), page.getRecords());
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
            R<List<SkuHasStockVo>> res = wareFeignService.getSkusHasStock(skuIdList);
            List<SkuHasStockVo> skuHasStockVos =
                    res.getData() == null ? new ArrayList<>() : res.getData();
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

            CategoryEntity byId = categoryService.getById(item.getCatalogId());
            model.setCatalogName(byId.getName());

            model.setAttrs(attrsList);

            return model;
        }).toList();

        R<Void> r = searchFeignService.productStatusUp(list);
        if (r.getCode() == 0) {
            this.baseMapper.updateSpuStatus(spuId, ProductConstant.ProductStatusEnum.UP.getCode());
        }
    }

    @Override
    @Transactional
    public void down(Long spuId) {
        if (spuId == null || this.getById(spuId) == null) {
            throw new ValidationException("spuId", "商品不存在");
        }

        // 幂等：已经是下架状态也照样发一遍，等于顺手修掉"库说下架、搜索还能搜到"
        spuInfoDao.updateSpuStatus(spuId, ProductConstant.ProductStatusEnum.DOWN.getCode());

        reliableMqPublisher.publish(
                MqConstant.Exchanges.PRODUCT_EVENT,
                MqConstant.RoutingKeys.PRODUCT_DOWN,
                new ProductDownTo(List.of(spuId)));

        log.info("商品下架完成：spuId=" + spuId);
    }

    @Override
    public SpuInfoEntity getSpuInfoBySkuId(Long skuId) {

        //先查询sku表里的数据
        SkuInfoEntity skuInfoEntity = skuInfoService.getById(skuId);

        //获得spuId
        Long spuId = skuInfoEntity.getSpuId();

        //再通过spuId查询spuInfo信息表里的数据
        SpuInfoEntity spuInfoEntity = this.baseMapper.selectById(spuId);

        //查询品牌表的数据获取品牌名
        BrandEntity brandEntity = brandService.getById(spuInfoEntity.getBrandId());
        spuInfoEntity.setBrandName(brandEntity.getName());

        return spuInfoEntity;
    }

    @Override
    public Map<Long, String> getUserNames(List<Long> list) {
        List<SpuInfoEntity> spuInfoEntities = baseMapper.selectByIds(list);
        return spuInfoEntities.stream().collect(Collectors.toMap(SpuInfoEntity::getId, SpuInfoEntity::getSpuName));
    }

    @Override
    public List<SpuSelectVO> getSpuSelect() {
        List<SpuInfoEntity> spuInfoEntities = baseMapper.selectList(null);
        return spuInfoEntities.stream().map(item -> {
            SpuSelectVO spuSelectVO = new SpuSelectVO();
            spuSelectVO.setId(item.getId());
            spuSelectVO.setName(item.getSpuName());
            return spuSelectVO;
        }).toList();
    }


}
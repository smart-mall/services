package product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import common.constant.ProductConstant;
import common.exception.BaseCodeEnum;
import common.exception.BaseException;
import common.exception.ValidationException;
import common.mq.MqConstant;
import common.to.SkuReductionTo;
import common.to.SpuBoundTo;
import common.to.mq.ProductDeletedTo;
import es.SkuEsModel;
import lombok.extern.slf4j.Slf4j;
import common.utils.PageUtils;
import common.utils.Query;
import common.utils.R;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import product.dao.*;
import product.entity.*;
import product.feign.CouponFeignService;
import product.feign.SearchFeignService;
import product.feign.WareFeignService;
import product.mq.MqMessageSender;
import product.service.*;
import product.vo.SkuHasStockVo;
import product.vo.SpuSelectVO;
import product.vo.SpuVO;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;


@Service("spuInfoService")
@Slf4j
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {

    /** 已上架商品的错误信息里最多列几个商品名，列多了前端 toast 显示不下 */
    private static final int MAX_UP_SHELVED_IN_MESSAGE = 3;

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
    private final MqMessageService mqMessageService;
    private final MqMessageSender mqMessageSender;
    private final ObjectMapper objectMapper;

    public SpuInfoServiceImpl(SpuInfoDao spuInfoDao, SpuInfoDescDao spuInfoDescDao, SpuImagesDao spuImagesDao, ProductAttrValueDao productAttrValueDao, ProductAttrValueService productAttrValueService, AttrDao attrDao, SkuInfoDao skuInfoDao, SkuImagesDao skuImagesDao, SkuSaleAttrValueDao skuSaleAttrValueDao, CouponFeignService couponFeignService, SkuInfoServiceImpl skuInfoService, BrandService brandService, CategoryService categoryService, AttrService attrService, WareFeignService wareFeignService, SearchFeignService searchFeignService, MqMessageService mqMessageService, MqMessageSender mqMessageSender, ObjectMapper objectMapper) {
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
        this.mqMessageService = mqMessageService;
        this.mqMessageSender = mqMessageSender;
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

        // 跨服务的清理（coupon 的积分/满减/打折/会员价、MinIO 里的图片）不在这个事务里直接做，
        // 而是落一条本地消息，提交后再投出去 —— 见 MqMessageSender 和 MqMessageResendTask。
        //
        // 为什么不直接调：远程删除是不可回滚的副作用，塞进本地事务只有两种结局，都是坏的 ——
        // 先删远程，本地一回滚就留下"商品还在、配置没了"；先提交再远程，远程失败就留下
        // 永远没人清理的孤儿。落消息是唯一让两边都有据可依的做法：
        // 这条 insert 和上面 7 张表的删除在同一个事务里，所以提交成功 == 商品没了 + 一定有一条待投递消息。
        String messageId = mqMessageService.savePending(
                MqConstant.Exchanges.PRODUCT_EVENT,
                MqConstant.RoutingKeys.PRODUCT_DELETED,
                new ProductDeletedTo(existingSpuIds, skuIds, imageUrls));

        // 提交之后再投。放进事务里发，一旦回滚消息已经出去了，消费方会去清理一个还活着的商品
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    mqMessageSender.send(messageId);
                }
            });
        } else {
            mqMessageSender.send(messageId);
        }

        log.info("级联删除商品完成：spuIds=" + existingSpuIds + "，skuIds=" + skuIds + "，messageId=" + messageId);
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

        String catalogId = (String) params.get("catalogId");
        if (catalogId != null && !catalogId.isEmpty()  && !"0".equals(catalogId)) {
            queryWrapper.eq(SpuInfoEntity::getCatalogId, catalogId);
        }


        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
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

            CategoryEntity byId = categoryService.getById(item.getCatalogId());
            model.setCatalogName(byId.getName());

            model.setAttrs(attrsList);

            return model;
        }).toList();

        R r = searchFeignService.productStatusUp(list);
        if (r.getCode() == 0) {
            this.baseMapper.updateSpuStatus(spuId, ProductConstant.ProductStatusEnum.UP.getCode());
        }
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
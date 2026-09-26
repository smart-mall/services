package product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
/**
 * 商品（spu）服务的默认实现，基于 MyBatis-Plus 的 {@code ServiceImpl} 读写 {@code pms_spu_info}。
 *
 * <p>写操作不止改本地库：新增商品一次写 7 张表并远程写 coupon 的积分与优惠；上架要推
 * Elasticsearch；删除前要问仓库能否删，跨服务的清理走本地消息表，提交后才投递。
 */
@Service("spuInfoService")
@Slf4j
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {

    /** 已上架商品的错误信息里最多列几个商品名，列多了前端 toast 显示不下。 */
    private static final int MAX_UP_SHELVED_IN_MESSAGE = 3;

    /** 仓库阻塞信息里最多列几个 sku，列多了前端 toast 显示不下。 */
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

    /**
     * 由容器注入七张子表的 Mapper、规格参数与 sku 服务、四个远程客户端与本地消息发布器构造。
     *
     * @param spuInfoDao spu 主表 Mapper，上架与下架时改发布状态
     * @param spuInfoDescDao spu 介绍 Mapper，新增商品时写描述图，删除时清
     * @param spuImagesDao spu 图集 Mapper，新增商品时写图集，删除时清
     * @param productAttrValueDao 规格参数值 Mapper，新增商品时写取值，删除时清
     * @param productAttrValueService 规格参数值服务，上架时按 spu 取取值
     * @param attrDao 属性 Mapper，写规格参数时回查属性名
     * @param skuInfoDao sku 主表 Mapper，新增商品时写 sku，删除时按 spu 取 sku
     * @param skuImagesDao sku 图集 Mapper，新增商品时写图集，删除时清
     * @param skuSaleAttrValueDao sku 销售属性值 Mapper，新增商品时写取值，删除时清
     * @param couponFeignService 优惠服务客户端，写积分信息与 sku 优惠
     * @param skuInfoService sku 服务实现类，上架时用接口上没有的 {@code getSkusBySpuId} 取 sku 列表
     * @param brandService 品牌服务，列表与索引文档里回填品牌名
     * @param categoryService 分类服务，列表与索引文档里回填分类名
     * @param attrService 属性服务，上架时筛出可作为检索条件的属性
     * @param wareFeignService 库存服务客户端，上架时查库存，删除前问仓库能否删
     * @param searchFeignService 搜索服务客户端，上架时推索引文档
     * @param reliableMqPublisher 本地消息发布器，删除与下架后投递事件
     */
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


    /** {@inheritDoc} */
    @Override
    public PageVO<SpuInfoEntity> queryPage(PageQuery query) {
        IPage<SpuInfoEntity> page = this.page(query.toPage());

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

    /**
     * {@inheritDoc}
     *
     * <p>本地 7 张表的写入在同一个事务里，中途任何一步抛异常都会整批回滚。
     */
    @Override
    @Transactional
    public void saveSpuInfo(SpuVO spuInfo) {
        // 1. spu 主表：后面所有子表都用它回填的自增 ID 关联
        SpuInfoEntity spuInfoEntity = new SpuInfoEntity();
        BeanUtils.copyProperties(spuInfo, spuInfoEntity);

        Date now = new Date();
        spuInfoEntity.setCreateTime(now);
        spuInfoEntity.setUpdateTime(now);

        spuInfoDao.insert(spuInfoEntity);


        // 2. 商品描述图：地址用逗号拼接存进一列
        List<String> discrip = spuInfo.getDescription();
        if (discrip != null && !discrip.isEmpty()) {
            SpuInfoDescEntity spuInfoDescEntity = new SpuInfoDescEntity();
            spuInfoDescEntity.setSpuId(spuInfoEntity.getId());
            spuInfoDescEntity.setDescription(String.join(",", discrip));

            spuInfoDescDao.insert(spuInfoDescEntity);
        }


        // 3. 商品图集
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


        // 4. 积分信息：远程写 coupon，失败要抛异常让本地整批回滚
        SpuBoundTo spuBoundTo = new SpuBoundTo();

        spuBoundTo.setSpuId(spuInfoEntity.getId());
        BeanUtils.copyProperties(spuInfo.getBounds(), spuBoundTo);
        R<Void> r1 = couponFeignService.saveSpuBounds(spuBoundTo);
        if (r1.getCode() != 0) {
            throw new RuntimeException("保存积分信息失败");
        }

        // 5. 规格参数：前端只传 attrId，属性名要回查属性表
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

        // 6. sku：逐个写 sku 主表、图集、销售属性与优惠信息
        List<SpuVO.Sku> skus = spuInfo.getSkus();
        if (skus != null && !skus.isEmpty()) {
            skus.forEach(sku -> {
                // 6.1 主图取 defaultImg 为 1 的那张；一张都没标时留空串
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

                // 6.2 sku 图集：空地址不入库，避免图集里出现取不到图的记录
                Long skuId = skuInfoEntity.getSkuId();

                List<SkuImagesEntity> skuImagesEntityStream = sku.getImages().stream().map(item -> {
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                    skuImagesEntity.setSkuId(skuId);
                    skuImagesEntity.setImgUrl(item.getImgUrl());
                    skuImagesEntity.setImgSort(item.getDefaultImg());

                    return skuImagesEntity;
                }).filter( item -> !item.getImgUrl().isEmpty()).toList();

                skuImagesDao.insert(skuImagesEntityStream);

                // 6.3 sku 销售属性
                List<SpuVO.Attr> attr = sku.getAttr();
                List<SkuSaleAttrValueEntity> skuSaleAttrValueEntityStream = attr.stream().map(item -> {
                    SkuSaleAttrValueEntity skuSaleAttrValueEntity = new SkuSaleAttrValueEntity();
                    BeanUtils.copyProperties(item, skuSaleAttrValueEntity);
                    skuSaleAttrValueEntity.setSkuId(skuId);
                    return skuSaleAttrValueEntity;
                }).toList();
                skuSaleAttrValueDao.insert(skuSaleAttrValueEntityStream);

                // 6.4 sku 优惠信息：两个优惠字段都没填就不发远程请求
                SkuReductionTo skuReductionTo = new SkuReductionTo();
                BeanUtils.copyProperties(sku, skuReductionTo);
                skuReductionTo.setSkuId(skuInfoEntity.getSkuId());

                if (skuReductionTo.getFullCount() > 0 || skuReductionTo.getFullPrice().compareTo(new BigDecimal("0")) > 0) {
                    R<Void> r = couponFeignService.saveSkuReduction(skuReductionTo);
                    if (r.getCode() != 0) {
                        // 不回滚本地事务：优惠数据在 coupon 侧，缺了不影响商品本身
                        log.error("保存sku优惠信息失败");
                    }
                }
            });

        }


    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void removeSpuInfo(List<Long> spuIds) {
        // 1. 入参归一，空集合直接拒绝
        List<Long> distinctSpuIds = spuIds == null
                ? List.of()
                : spuIds.stream().filter(Objects::nonNull).distinct().toList();
        if (distinctSpuIds.isEmpty()) {
            throw new ValidationException("ids", "请选择要删除的商品");
        }

        // 2. 查出待删商品，拦下已上架的那些
        List<SpuInfoEntity> spus = this.listByIds(distinctSpuIds);

        // 上架中的商品不许删：它的 sku 还在 ES 里挂着，删了搜索结果里会留下打不开的商品。
        // 整批拒绝而不是删一半 —— 只删一半的话，调用方拿到的"成功"里混着没删掉的 id
        List<SpuInfoEntity> upShelved = spus.stream()
                .filter(spu -> Objects.equals(spu.getPublishStatus(),
                        ProductConstant.ProductStatusEnum.UP.getCode()))
                .toList();
        if (!upShelved.isEmpty()) {
            throw new BaseException(BaseCodeEnum.PRODUCT_UP_SHELVED_CANNOT_DELETE,
                    describeUpShelved(upShelved));
        }

        // 3. 查不到的 id 静默跳过：重复提交、id 传错都不该报错，删除本身是幂等的
        List<Long> existingSpuIds = spus.stream().map(SpuInfoEntity::getId).toList();
        if (existingSpuIds.isEmpty()) {
            return;
        }

        List<SkuInfoEntity> skus = skuInfoDao.selectList(new LambdaQueryWrapper<SkuInfoEntity>()
                .in(SkuInfoEntity::getSpuId, existingSpuIds));
        List<Long> skuIds = skus.stream().map(SkuInfoEntity::getSkuId).toList();

        // 4. 仓库侧还有库存或没走完的采购需求就不许删：那是真实的货和在途单据，
        // 商品先没了它们就成无主数据
        assertWareCanDelete(skuIds);

        // 5. 这些地址指向 MinIO 里的对象，必须在删行之前收集 —— 行一删就再也找不到它们了
        List<String> imageUrls = collectImageUrls(existingSpuIds, skus);

        // 6. 先删子表再删主表：库里没有外键，顺序不影响最终结果，但反过来会出现
        // "主表已经没了、子表还在"的中间状态，看着就是脏数据
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

        // 7. 跨服务的清理（coupon 的优惠数据、MinIO 图片）落一条本地消息，与上面 7 张表的删除同事务，
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
     * <p>读不到 ware 时按"不能删"处理：放行可能删掉还有库存的商品，那是不可逆的；
     * 拦下来最坏只是暂时删不掉。所以用独立的错误码，不和"确实有引用"混成一个 ——
     * 混了用户会去清数据，而实际上什么都不用清。
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

    /**
     * 把仓库返回的阻塞清单拼成一句人能读的话。
     *
     * <p>列多了前端 toast 显示不下，所以只取前 {@code MAX_WARE_BLOCKERS_IN_MESSAGE} 条。
     *
     * @param blockers 仓库返回的阻塞清单，不能为空
     * @return 形如「商品在仓库还有库存或未完成的采购需求（…），请先处理后再删除」的提示语
     */
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
     * 所以照样收集。
     *
     * @param spuIds 待删除的 spu ID 列表
     * @param skus 这些 spu 下的 sku 列表
     * @return 去重后的文件地址列表；一个都没有时返回空列表
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

        // 商品描述存的是逗号拼接的一组地址，见 saveSpuInfo 里的 String.join(",", discrip)
        spuInfoDescDao.selectList(new LambdaQueryWrapper<SpuInfoDescEntity>()
                        .in(SpuInfoDescEntity::getSpuId, spuIds))
                .forEach(desc -> urls.addAll(splitDescription(desc.getDescription())));

        return urls.stream().filter(StringUtils::hasText).distinct().toList();
    }

    /**
     * 按逗号拆开商品描述里拼接的图片地址。
     *
     * @param description 逗号拼接的描述图地址，允许为 {@code null} 或空串
     * @return 拆分并去掉空白项后的地址列表；没有有效地址时返回空列表
     */
    private static List<String> splitDescription(String description) {
        if (!StringUtils.hasText(description)) {
            return List.of();
        }
        return Arrays.stream(description.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    /**
     * 把已上架的商品名拼成一句人能读的提示。
     *
     * <p>只回一句"有商品已上架"调用方还得自己猜是哪一个，所以带上名字；截断是为了让 msg
     * 塞得进前端 toast。
     *
     * @param upShelved 已上架的商品列表，不能为空
     * @return 形如「商品「A、B」已上架，请先下架再删除」的提示语
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

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
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
            // 库存服务异常不阻断上架：拿不到库存一律按有货写索引
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

    /**
     * {@inheritDoc}
     *
     * <p>状态更新与下架事件落库在同一个事务里，事件随事务提交后才投递。
     */
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

    /** {@inheritDoc} */
    @Override
    public SpuInfoEntity getSpuInfoBySkuId(Long skuId) {

        SkuInfoEntity skuInfoEntity = skuInfoService.getById(skuId);

        Long spuId = skuInfoEntity.getSpuId();

        SpuInfoEntity spuInfoEntity = this.baseMapper.selectById(spuId);

        BrandEntity brandEntity = brandService.getById(spuInfoEntity.getBrandId());
        spuInfoEntity.setBrandName(brandEntity.getName());

        return spuInfoEntity;
    }

    /** {@inheritDoc} */
    @Override
    public Map<Long, String> getUserNames(List<Long> list) {
        List<SpuInfoEntity> spuInfoEntities = baseMapper.selectByIds(list);
        return spuInfoEntities.stream().collect(Collectors.toMap(SpuInfoEntity::getId, SpuInfoEntity::getSpuName));
    }

    /** {@inheritDoc} */
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
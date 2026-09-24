package product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.exception.BaseCodeEnum;
import common.exception.BaseException;
import common.utils.PageUtils;
import common.utils.Query;
import common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import product.dao.BrandDao;
import product.dao.SpuInfoDao;
import product.entity.BrandEntity;
import product.entity.SpuInfoEntity;
import product.feign.ThirdPartyFeignService;
import product.service.BrandService;
import product.service.CategoryBrandRelationService;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;


@Service("brandService")
@Slf4j
public class BrandServiceImpl extends ServiceImpl<BrandDao, BrandEntity> implements BrandService {
    private final CategoryBrandRelationService categoryBrandRelationService;
    private final ThirdPartyFeignService thirdPartyFeignService;

    // 注入 DAO 而不是 SpuInfoService：SpuInfoServiceImpl 依赖 BrandService，会构造器循环
    private final SpuInfoDao spuInfoDao;

    public BrandServiceImpl(CategoryBrandRelationService categoryBrandRelationService,
                            ThirdPartyFeignService thirdPartyFeignService,
                            SpuInfoDao spuInfoDao) {
        this.categoryBrandRelationService = categoryBrandRelationService;
        this.thirdPartyFeignService = thirdPartyFeignService;
        this.spuInfoDao = spuInfoDao;
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String key = (String)params.get("key");
        LambdaQueryWrapper<BrandEntity> wrapper = new LambdaQueryWrapper<>();

        if (key != null && !key.isEmpty()) {
            wrapper.like(BrandEntity::getName, key)
                    .or()
                    .like(BrandEntity::getBrandId, key);
        }

        IPage<BrandEntity> page = this.page(
                new Query<BrandEntity>().getPage(params),
                 wrapper
        );

        return new PageUtils(page);
    }

    @Override
    @Transactional
    public void updateDetail(BrandEntity brand) {
        log.debug("修改文件");
        String oldPath = this.getById(brand.getBrandId()).getLogo();
        if (StringUtils.hasText(oldPath) && !oldPath.equals(brand.getLogo())) {
            R r = thirdPartyFeignService.deleteFile(List.of(oldPath));
            if (r.getCode() != 0) {
                throw new BaseException("删除失败" + r.getMsg());
            }
        }
        log.debug("修改品牌信息");
        this.updateById(brand);
        log.debug("修改分类品牌关联表");
        categoryBrandRelationService.updateBrand(brand.getBrandId(), brand.getName());

    }

    /**
     * 删除品牌。品牌下还有商品时整批拒绝；品牌与分类的关联行跟着一起删。
     */
    @Override
    @Transactional
    public void deleteByIds(List<Long> list) {
        List<Long> brandIds = list == null ? List.of()
                : list.stream().filter(Objects::nonNull).distinct().toList();
        if (brandIds.isEmpty()) {
            return;
        }

        List<BrandEntity> brandEntities = baseMapper.selectByIds(brandIds);
        if (brandEntities.isEmpty()) {
            return;
        }
        List<Long> existingIds = brandEntities.stream().map(BrandEntity::getBrandId).toList();

        // 先校验，后删文件：文件删了行却留下的话，logo 就指向一个 404
        ensureNoSpu(brandEntities, existingIds);

        // 关联行只表示"这个品牌挂在哪些分类下"，品牌没了它就没有意义，不用拦着让用户手工去摘
        categoryBrandRelationService.deleteByBrandIds(existingIds);

        List<String> objectNames = brandEntities.stream().map(BrandEntity::getLogo).toList();
        R r = thirdPartyFeignService.deleteFile(objectNames);
        if (r.getCode() != 0) {
            throw new BaseException("删除失败" + r.getMsg());
        }
        this.removeByIds(existingIds);
    }

    /**
     * 品牌下还有商品时拒绝删除。pms_sku_info.brand_id 是跟随 spu 的冗余列，不用单独查。
     */
    private void ensureNoSpu(List<BrandEntity> brands, List<Long> brandIds) {
        Long spuCount = spuInfoDao.selectCount(
                new LambdaQueryWrapper<SpuInfoEntity>().in(SpuInfoEntity::getBrandId, brandIds));
        if (spuCount == null || spuCount == 0) {
            return;
        }

        String names = brands.stream().map(BrandEntity::getName).collect(Collectors.joining("、"));
        throw new BaseException(BaseCodeEnum.BRAND_IN_USE,
                "品牌【" + names + "】下还有 " + spuCount + " 个商品，请先处理后再删除");
    }

}
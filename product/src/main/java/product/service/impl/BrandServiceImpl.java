package product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.exception.BaseCodeEnum;
import common.exception.BaseException;
import common.vo.PageVO;
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
import java.util.Objects;
import java.util.stream.Collectors;


import common.query.KeyPageQuery;
/**
 * 品牌服务的默认实现，基于 MyBatis-Plus 的 {@code ServiceImpl} 读写 {@code pms_brand}。
 *
 * <p>logo 存在 MinIO：换图时删掉被替换的对象，删除品牌时删掉全部 logo。品牌名冗余在
 * {@code pms_category_brand_relation} 里，改名要顺带回写。
 */
@Service("brandService")
@Slf4j
public class BrandServiceImpl extends ServiceImpl<BrandDao, BrandEntity> implements BrandService {
    private final CategoryBrandRelationService categoryBrandRelationService;
    private final ThirdPartyFeignService thirdPartyFeignService;

    // 注入 DAO 而不是 SpuInfoService：SpuInfoServiceImpl 依赖 BrandService，会构造器循环
    private final SpuInfoDao spuInfoDao;

    /**
     * 由容器注入品牌分类关联服务、三方文件客户端与 spu 主表 Mapper 构造。
     *
     * @param categoryBrandRelationService 品牌分类关联服务，改名后回写关联表里的品牌名
     * @param thirdPartyFeignService 三方文件客户端，换 logo 与删品牌时清理 MinIO 对象
     * @param spuInfoDao spu 主表 Mapper，删除品牌前判断品牌下是否还有商品
     */
    public BrandServiceImpl(CategoryBrandRelationService categoryBrandRelationService,
                            ThirdPartyFeignService thirdPartyFeignService,
                            SpuInfoDao spuInfoDao) {
        this.categoryBrandRelationService = categoryBrandRelationService;
        this.thirdPartyFeignService = thirdPartyFeignService;
        this.spuInfoDao = spuInfoDao;
    }

    /** {@inheritDoc} */
    @Override
    public PageVO<BrandEntity> queryPage(KeyPageQuery query) {
        String key = query.getKey();
        LambdaQueryWrapper<BrandEntity> wrapper = new LambdaQueryWrapper<>();

        if (key != null && !key.isEmpty()) {
            wrapper.like(BrandEntity::getName, key)
                    .or()
                    .like(BrandEntity::getBrandId, key);
        }

        IPage<BrandEntity> page = this.page(
                query.toPage(),
                 wrapper
        );

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void updateDetail(BrandEntity brand) {
        log.debug("修改文件");
        String oldPath = this.getById(brand.getBrandId()).getLogo();
        if (StringUtils.hasText(oldPath) && !oldPath.equals(brand.getLogo())) {
            R<List<String>> r = thirdPartyFeignService.deleteFile(List.of(oldPath));
            if (r.getCode() != 0) {
                throw new BaseException("删除失败" + r.getMsg());
            }
        }
        log.debug("修改品牌信息");
        this.updateById(brand);
        log.debug("修改分类品牌关联表");
        categoryBrandRelationService.updateBrand(brand.getBrandId(), brand.getName());

    }

    /** {@inheritDoc} */
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
        R<List<String>> r = thirdPartyFeignService.deleteFile(objectNames);
        if (r.getCode() != 0) {
            throw new BaseException("删除失败" + r.getMsg());
        }
        this.removeByIds(existingIds);
    }

    /**
     * 校验这些品牌下没有商品，有则整批拒绝删除。
     *
     * <p>只查 spu 主表：{@code pms_sku_info.brand_id} 是跟随 spu 的冗余列，不用单独查。
     *
     * @param brands 待删除的品牌实体，用于拼错误信息里的品牌名
     * @param brandIds 待删除的品牌 ID 列表
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
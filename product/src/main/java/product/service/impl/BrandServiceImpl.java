package product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.exception.BaseException;
import common.utils.PageUtils;
import common.utils.Query;
import common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import product.dao.BrandDao;
import product.entity.BrandEntity;
import product.feign.ThirdPartyFeignService;
import product.service.BrandService;
import product.service.CategoryBrandRelationService;

import java.util.List;
import java.util.Map;


@Service("brandService")
@Slf4j
public class BrandServiceImpl extends ServiceImpl<BrandDao, BrandEntity> implements BrandService {
    private final CategoryBrandRelationService categoryBrandRelationService;
    private final ThirdPartyFeignService thirdPartyFeignService;

    public BrandServiceImpl(CategoryBrandRelationService categoryBrandRelationService, ThirdPartyFeignService thirdPartyFeignService) {
        this.categoryBrandRelationService = categoryBrandRelationService;
        this.thirdPartyFeignService = thirdPartyFeignService;
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

    @Override
    public void deleteByIds(List<Long> list) {
        List<BrandEntity> brandEntities = baseMapper.selectByIds(list);
        List<String> objectNames = brandEntities.stream().map(BrandEntity::getLogo).toList();
        R r = thirdPartyFeignService.deleteFile(objectNames);
        if (r.getCode() != 0) {
            throw new BaseException("删除失败" + r.getMsg());
        }
        this.removeByIds(list);
    }

}
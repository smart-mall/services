package product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.exception.BaseException;
import common.utils.PageUtils;
import common.utils.Query;
import common.utils.R;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import product.dao.AttrAttrgroupRelationDao;
import product.dao.AttrGroupDao;
import product.entity.AttrAttrgroupRelationEntity;
import product.entity.AttrEntity;
import product.entity.AttrGroupEntity;
import product.entity.CategoryEntity;
import product.feign.ThirdPartyFeignService;
import product.service.AttrGroupService;
import product.service.AttrService;
import product.service.CategoryService;
import product.vo.AttrGroupRelationVO;
import product.vo.AttrGroupRespVO;
import product.vo.AttrGroupWithAttrsVO;
import product.vo.SpuItemAttrGroupVo;

import java.util.List;
import java.util.Map;


@Service("attrGroupService")
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupDao, AttrGroupEntity> implements AttrGroupService {
    private final AttrAttrgroupRelationDao relationDao;
    private final AttrService attrService;
    private final CategoryService categoryService;
    private final ThirdPartyFeignService thirdPartyFeignService;

    public AttrGroupServiceImpl(AttrAttrgroupRelationDao relationDao, AttrService attrService, CategoryService categoryService, ThirdPartyFeignService thirdPartyFeignService) {
        this.relationDao = relationDao;
        this.attrService = attrService;
        this.categoryService = categoryService;
        this.thirdPartyFeignService = thirdPartyFeignService;
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrGroupEntity> page = this.page(
                new Query<AttrGroupEntity>().getPage(params),
                new QueryWrapper<>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params, Long categoryId) {
        List<CategoryEntity> list = categoryService.list();

        LambdaQueryWrapper<AttrGroupEntity> lambdaQueryWrapper = new LambdaQueryWrapper<>();

        if (categoryId != null && categoryId != 0) {
            lambdaQueryWrapper.eq(AttrGroupEntity::getCatalogId, categoryId);
        }

        String key = (String) params.get("key");

        if (key != null && !key.isEmpty()) {
            lambdaQueryWrapper.like(AttrGroupEntity::getAttrGroupName, key)
                        .or()
                        .eq(AttrGroupEntity::getAttrGroupId, key);
        }
        IPage<AttrGroupEntity> page = this.page(
                new Query<AttrGroupEntity>().getPage(params),
                lambdaQueryWrapper
        );

        List<AttrGroupRespVO> attrGroupRespVOS = page.getRecords().stream().map(attrGroupEntity -> {
            AttrGroupRespVO attrGroupRespVO = new AttrGroupRespVO();
            BeanUtils.copyProperties(attrGroupEntity, attrGroupRespVO);
            attrGroupRespVO.setCatalogName(list.stream().filter(categoryEntity -> categoryEntity.getCatId().equals(attrGroupEntity.getCatalogId())).findFirst().get().getName());
            return attrGroupRespVO;
        }).toList();
        PageUtils pageUtils = new PageUtils(page);
        pageUtils.setList(attrGroupRespVOS);
        return pageUtils;
    }

    @Override
    public void deleteRelation(AttrGroupRelationVO[] vos) {
        LambdaQueryWrapper<AttrAttrgroupRelationEntity> lambdaQueryWrapper = new LambdaQueryWrapper<>();

        for (AttrGroupRelationVO vo : vos) {
            lambdaQueryWrapper.eq(AttrAttrgroupRelationEntity::getAttrId, vo.getAttrId())
                    .eq(AttrAttrgroupRelationEntity::getAttrGroupId, vo.getAttrGroupId());
            relationDao.delete(lambdaQueryWrapper);
        }
    }

    @Override
    public List<AttrGroupWithAttrsVO> getAttrGroupWithAttrs(Long catalogId) {
        List<AttrGroupEntity> attrGroupEntities = baseMapper.selectList(new LambdaQueryWrapper<>(AttrGroupEntity.class).eq(AttrGroupEntity::getCatalogId, catalogId));

        return attrGroupEntities.stream().map(attrGroupEntity -> {
            AttrGroupWithAttrsVO attrGroupWithAttrsVO = new AttrGroupWithAttrsVO();
            BeanUtils.copyProperties(attrGroupEntity, attrGroupWithAttrsVO);
            List<AttrEntity> relationAttr = attrService.getRelationAttr(attrGroupEntity.getAttrGroupId());
            attrGroupWithAttrsVO.setAttrs(relationAttr);
            return attrGroupWithAttrsVO;
        }).toList();
    }

    @Override
    public List<SpuItemAttrGroupVo> getAttrGroupWithAttrsBySpuId(Long spuId, Long catalogId) {

        //1、查出当前spu对应的所有属性的分组信息以及当前分组下的所有属性对应的值
        AttrGroupDao baseMapper = this.getBaseMapper();
        List<SpuItemAttrGroupVo> vos = baseMapper.getAttrGroupWithAttrsBySpuId(spuId,catalogId);

        return vos;
    }

    @Override
    public void deleteByIds(List<Long> list) {
        List<AttrGroupEntity> attrGroupEntities = baseMapper.selectByIds(list);
        List<String> objectNames = attrGroupEntities.stream().map(AttrGroupEntity::getIcon).toList();
        R r = thirdPartyFeignService.deleteFile(objectNames);
        if (r.getCode() != 0) {
            throw new BaseException("删除失败" + r.getMsg());
        }
        this.removeByIds(list);
    }

    @Override
    public void updateDetail(AttrGroupEntity attrGroup) {
        log.debug("修改文件");
        String oldPath = this.getById(attrGroup.getAttrGroupId()).getIcon();
        if (StringUtils.hasText(oldPath) && !oldPath.equals(attrGroup.getIcon())) {
            R r = thirdPartyFeignService.deleteFile(List.of(oldPath));
            if (r.getCode() != 0) {
                throw new BaseException("删除失败" + r.getMsg());
            }
        }
        log.debug("修改品牌信息");
        this.updateById(attrGroup);
    }
}
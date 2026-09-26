package product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.exception.BaseException;
import common.vo.PageVO;
import common.utils.R;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import java.util.Objects;


import common.query.KeyPageQuery;
import common.query.PageQuery;
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
    public PageVO<AttrGroupEntity> queryPage(PageQuery query) {
        IPage<AttrGroupEntity> page = this.page(query.toPage());

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

    @Override
    public PageVO<AttrGroupRespVO> queryPage(KeyPageQuery query, Long categoryId) {
        List<CategoryEntity> list = categoryService.list();

        LambdaQueryWrapper<AttrGroupEntity> lambdaQueryWrapper = new LambdaQueryWrapper<>();

        if (categoryId != null && categoryId != 0) {
            lambdaQueryWrapper.eq(AttrGroupEntity::getCatalogId, categoryId);
        }

        String key = query.getKey();

        if (key != null && !key.isEmpty()) {
            lambdaQueryWrapper.like(AttrGroupEntity::getAttrGroupName, key)
                        .or()
                        .eq(AttrGroupEntity::getAttrGroupId, key);
        }
        IPage<AttrGroupEntity> page = this.page(
                query.toPage(),
                lambdaQueryWrapper
        );

        List<AttrGroupRespVO> attrGroupRespVOS = page.getRecords().stream().map(attrGroupEntity -> {
            AttrGroupRespVO attrGroupRespVO = new AttrGroupRespVO();
            BeanUtils.copyProperties(attrGroupEntity, attrGroupRespVO);
            attrGroupRespVO.setCatalogName(list.stream().filter(categoryEntity -> categoryEntity.getCatId().equals(attrGroupEntity.getCatalogId())).findFirst().get().getName());
            return attrGroupRespVO;
        }).toList();
        return new PageVO<>(page.getTotal(), attrGroupRespVOS);
    }

    /**
     * 移除属性组与属性的关联。每条单独建 wrapper —— 复用同一个的话条件会累积成
     * attr_id=a1 AND attr_group_id=g1 AND attr_id=a2 AND ...，第 2 条之后永远匹配不到行。
     */
    @Override
    @Transactional
    public void deleteRelation(AttrGroupRelationVO[] vos) {
        if (vos == null) {
            return;
        }

        for (AttrGroupRelationVO vo : vos) {
            relationDao.delete(new LambdaQueryWrapper<AttrAttrgroupRelationEntity>()
                    .eq(AttrAttrgroupRelationEntity::getAttrId, vo.getAttrId())
                    .eq(AttrAttrgroupRelationEntity::getAttrGroupId, vo.getAttrGroupId()));
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

    /**
     * 删除属性组。属性组与属性的关联行跟着一起删 —— 组没了，这层归属关系本身就没有意义了，
     * 属性行不受影响。没有别的数据引用属性组，所以这里不需要拦截。
     */
    @Override
    @Transactional
    public void deleteByIds(List<Long> list) {
        List<Long> groupIds = list == null ? List.of()
                : list.stream().filter(Objects::nonNull).distinct().toList();
        if (groupIds.isEmpty()) {
            return;
        }

        List<AttrGroupEntity> attrGroupEntities = baseMapper.selectByIds(groupIds);
        if (attrGroupEntities.isEmpty()) {
            return;
        }
        List<Long> existingIds = attrGroupEntities.stream().map(AttrGroupEntity::getAttrGroupId).toList();

        relationDao.delete(new LambdaQueryWrapper<AttrAttrgroupRelationEntity>()
                .in(AttrAttrgroupRelationEntity::getAttrGroupId, existingIds));

        List<String> objectNames = attrGroupEntities.stream().map(AttrGroupEntity::getIcon).toList();
        R<List<String>> r = thirdPartyFeignService.deleteFile(objectNames);
        if (r.getCode() != 0) {
            throw new BaseException("删除失败" + r.getMsg());
        }
        this.removeByIds(existingIds);
    }

    @Override
    public void updateDetail(AttrGroupEntity attrGroup) {
        log.debug("修改文件");
        String oldPath = this.getById(attrGroup.getAttrGroupId()).getIcon();
        if (StringUtils.hasText(oldPath) && !oldPath.equals(attrGroup.getIcon())) {
            R<List<String>> r = thirdPartyFeignService.deleteFile(List.of(oldPath));
            if (r.getCode() != 0) {
                throw new BaseException("删除失败" + r.getMsg());
            }
        }
        log.debug("修改品牌信息");
        this.updateById(attrGroup);
    }
}
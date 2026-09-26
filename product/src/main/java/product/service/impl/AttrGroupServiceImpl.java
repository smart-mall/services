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
/**
 * 属性分组服务的默认实现，基于 MyBatis-Plus 的 {@code ServiceImpl} 读写 {@code pms_attr_group}。
 *
 * <p>分组图标存在 MinIO，换图与删除分组时同步清理对象；分组与属性的绑定关系记在
 * {@code pms_attr_attrgroup_relation}，删除分组时一并清掉。
 */
@Service("attrGroupService")
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupDao, AttrGroupEntity> implements AttrGroupService {
    private final AttrAttrgroupRelationDao relationDao;
    private final AttrService attrService;
    private final CategoryService categoryService;
    private final ThirdPartyFeignService thirdPartyFeignService;

    /**
     * 由容器注入关联 Mapper、属性服务、分类服务与三方文件客户端构造。
     *
     * @param relationDao 属性分组关联 Mapper，删除分组与解绑时清关联行
     * @param attrService 属性服务，按分组取已绑定的属性
     * @param categoryService 分类服务，分页结果里回填分组所属分类名
     * @param thirdPartyFeignService 三方文件客户端，换图标与删分组时清理 MinIO 对象
     */
    public AttrGroupServiceImpl(AttrAttrgroupRelationDao relationDao, AttrService attrService, CategoryService categoryService, ThirdPartyFeignService thirdPartyFeignService) {
        this.relationDao = relationDao;
        this.attrService = attrService;
        this.categoryService = categoryService;
        this.thirdPartyFeignService = thirdPartyFeignService;
    }

    /** {@inheritDoc} */
    @Override
    public PageVO<AttrGroupEntity> queryPage(PageQuery query) {
        IPage<AttrGroupEntity> page = this.page(query.toPage());

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

    /** {@inheritDoc} */
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
     * {@inheritDoc}
     *
     * <p>每条关联单独建 wrapper：复用同一个会把条件累积成
     * {@code attr_id=a1 AND attr_group_id=g1 AND attr_id=a2 ...}，第二条起永远匹配不到行。
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

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
    @Override
    public List<SpuItemAttrGroupVo> getAttrGroupWithAttrsBySpuId(Long spuId, Long catalogId) {

        AttrGroupDao baseMapper = this.getBaseMapper();
        List<SpuItemAttrGroupVo> vos = baseMapper.getAttrGroupWithAttrsBySpuId(spuId,catalogId);

        return vos;
    }

    /**
     * {@inheritDoc}
     *
     * <p>没有别的数据引用属性分组，所以不做引用校验，直接连关联行与图标对象一起删。
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

    /**
     * {@inheritDoc}
     *
     * <p>先删被替换的图标、再更新分组行，本地不加事务：图标删除失败时更新还没执行，分组行仍是原值。
     */
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
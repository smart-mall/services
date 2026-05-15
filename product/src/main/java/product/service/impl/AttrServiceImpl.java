package product.service.impl;

import ch.qos.logback.core.util.StringUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.constant.ProductConstant;
import common.exception.BaseException;
import common.utils.PageUtils;
import common.utils.Query;
import common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import product.dao.AttrAttrgroupRelationDao;
import product.dao.AttrDao;
import product.dao.AttrGroupDao;
import product.dao.CategoryDao;
import product.entity.*;
import product.feign.ThirdPartyFeignService;
import product.service.AttrService;
import product.service.CategoryService;
import product.vo.AttrRespVO;
import product.vo.AttrVO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Service("attrService")
@Slf4j
public class AttrServiceImpl extends ServiceImpl<AttrDao, AttrEntity> implements AttrService {
    private final AttrAttrgroupRelationDao relationDao;

    private final AttrGroupDao attrGroupDao;

    private final CategoryDao categoryDao;

    private final CategoryService categoryService;

    private final ThirdPartyFeignService thirdPartyFeignService;

    public AttrServiceImpl(AttrAttrgroupRelationDao relationDao, AttrGroupDao attrGroupDao, CategoryDao categoryDao, CategoryService categoryService, ThirdPartyFeignService thirdPartyFeignService) {
        this.relationDao = relationDao;
        this.attrGroupDao = attrGroupDao;
        this.categoryDao = categoryDao;
        this.categoryService = categoryService;
        this.thirdPartyFeignService = thirdPartyFeignService;
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                new QueryWrapper<>()
        );

        return new PageUtils(page);
    }

    @Override
    public void saveAttr(AttrVO attr) {
        AttrEntity attrEntity = new AttrEntity();
        BeanUtils.copyProperties(attr, attrEntity);
        this.save(attrEntity);

        if (attr.getAttrType() == ProductConstant.AttrEnum.TYPE_BASE.getCode()) {
            AttrAttrgroupRelationEntity relation = new AttrAttrgroupRelationEntity();
            relation.setAttrId(attrEntity.getAttrId());
            relation.setAttrGroupId(attr.getAttrGroupId());
            relationDao.insert(relation);
        }
    }

    @Override
    public PageUtils queryBaseAttrPage(Map<String, Object> params, Long categoryId, String attrType) {

        LambdaQueryWrapper<AttrEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AttrEntity::getAttrType,
                "base".equalsIgnoreCase(attrType) ?
                        ProductConstant.AttrEnum.TYPE_BASE.getCode() :
                        ProductConstant.AttrEnum.TYPE_SALE.getCode()
        );

        String key = (String) params.get("key");
        if (!StringUtil.isNullOrEmpty(key)) {
            wrapper.like(AttrEntity::getAttrId, key)
                    .or()
                    .like(AttrEntity::getAttrName, key);
        }
        if (categoryId != null && categoryId != 0) {
            wrapper.eq(AttrEntity::getCatalogId, categoryId);
        }

        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                wrapper
        );

        List<AttrAttrgroupRelationEntity> relationEntities = relationDao.selectList(null);
        List<AttrGroupEntity> attrGroupEntities = attrGroupDao.selectList(null);
        List<CategoryEntity> categoryEntities = categoryDao.selectList(null);

        List<AttrRespVO> list = page.getRecords().stream().map(attr -> {
            AttrRespVO attrRespVO = new AttrRespVO();
            BeanUtils.copyProperties(attr, attrRespVO);
            // 先获取组id
            Optional<AttrAttrgroupRelationEntity> first = relationEntities.stream().filter(relation -> relation.getAttrId().equals(attr.getAttrId())).findFirst();
            // 找到 组
            if (first.isPresent()) {
                AttrAttrgroupRelationEntity relation = first.get();
                Optional<AttrGroupEntity> first1 = attrGroupEntities.stream().filter(attrGroup -> attrGroup.getAttrGroupId().equals(relation.getAttrGroupId())).findFirst();
                if (first1.isPresent()) {
                    AttrGroupEntity attrGroup = first1.get();
                    attrRespVO.setGroupName(attrGroup.getAttrGroupName());
                    attrRespVO.setAttrGroupId(attrGroup.getAttrGroupId());
                }

            }


            Optional<CategoryEntity> first2 = categoryEntities.stream().filter(it -> it.getCatId().equals(attr.getCatalogId())).findFirst();
            first2.ifPresent(categoryEntity -> attrRespVO.setCatalogName(categoryEntity.getName()));

            return attrRespVO;
        }).toList();

        PageUtils pageUtils = new PageUtils(page);
        pageUtils.setList(list);
        return pageUtils;
    }

    @Override
    public AttrRespVO getAttrInfo(Long attrId) {
        AttrRespVO attrRespVO = new AttrRespVO();
        AttrEntity attrEntity = this.getById(attrId);
        BeanUtils.copyProperties(attrEntity, attrRespVO);

        AttrAttrgroupRelationEntity relationEntity = relationDao.selectOne(
                new LambdaQueryWrapper<>(AttrAttrgroupRelationEntity.class)
                        .eq(AttrAttrgroupRelationEntity::getAttrId, attrId)
        );
        if (relationEntity != null) {
            attrRespVO.setAttrGroupId(relationEntity.getAttrGroupId());
            AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(relationEntity.getAttrGroupId());
            attrRespVO.setGroupName(attrGroupEntity.getAttrGroupName());
            attrRespVO.setCatalogId(attrGroupEntity.getCatalogId());
        }

        Long catalogId = attrEntity.getCatalogId();
        attrRespVO.setCatalogPath(categoryService.findcatalogIds(catalogId));
        attrRespVO.setCatalogName(categoryDao.selectById(catalogId).getName());
        return attrRespVO;
    }

    @Override
    @Transactional
    public void updateAttr(AttrVO attr) {
        AttrEntity attrEntity = new AttrEntity();
        BeanUtils.copyProperties(attr, attrEntity);
        log.debug("更新文件");
        String oldPath = this.getById(attr.getAttrId()).getIcon();
        if (StringUtils.hasText(oldPath) && !oldPath.equals(attr.getIcon())) {
            R r = thirdPartyFeignService.deleteFile(List.of(oldPath));
            if (r.getCode() != 0) {
                throw new BaseException("删除失败" + r.getMsg());
            }
        }

        log.debug("更新基础信息");
        this.updateById(attrEntity);

        if (attr.getAttrType() != ProductConstant.AttrEnum.TYPE_BASE.getCode()) {
            return;
        }

        AttrAttrgroupRelationEntity relation = new AttrAttrgroupRelationEntity();
        relation.setAttrId(attrEntity.getAttrId());
        relation.setAttrGroupId(attr.getAttrGroupId());

        // 查询是否存在
        AttrAttrgroupRelationEntity relationEntity = relationDao.selectOne(
                new LambdaQueryWrapper<>(AttrAttrgroupRelationEntity.class)
                        .eq(AttrAttrgroupRelationEntity::getAttrId, attr.getAttrId())
        );

        if (relationEntity == null) {
            relationDao.insert(relation);
        } else {
            relationDao.update(relation,
                    new LambdaQueryWrapper<>(AttrAttrgroupRelationEntity.class)
                            .eq(AttrAttrgroupRelationEntity::getAttrId, attr.getAttrId())
            );
        }
        log.debug("更新关联信息");
    }

    @Override
    public List<AttrEntity> getRelationAttr(Long attrGroupId) {
        List<AttrAttrgroupRelationEntity> relations = relationDao.selectList(
                new LambdaQueryWrapper<>(AttrAttrgroupRelationEntity.class)
                        .eq(AttrAttrgroupRelationEntity::getAttrGroupId, attrGroupId)
        );

        List<Long> attrIds = relations.stream().map(AttrAttrgroupRelationEntity::getAttrId).toList();
        if (attrIds.isEmpty()) {
            return new ArrayList<>();
        }
        return this.listByIds(attrIds);
    }

    @Override
    public PageUtils getNoRelationAttr(Long attrGroupId, Map<String, Object> params) {
        AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attrGroupId);
        log.debug("获取分组信息：{}", attrGroupEntity);

        Long catalogId = attrGroupEntity.getCatalogId();

        List<AttrGroupEntity> attrGroupEntities = attrGroupDao.selectList(
                new LambdaQueryWrapper<>(AttrGroupEntity.class)
                        .eq(AttrGroupEntity::getCatalogId, catalogId)
        );
        log.debug("获取分类下的所有属性组：{}", attrGroupEntities);

        List<Long> attrGroupIds = attrGroupEntities.stream().map(AttrGroupEntity::getAttrGroupId).toList();

        List<AttrAttrgroupRelationEntity> relationEntities = relationDao.selectList(
                new LambdaQueryWrapper<>(AttrAttrgroupRelationEntity.class)
                        .in(AttrAttrgroupRelationEntity::getAttrGroupId, attrGroupIds)
        );
        log.debug("获取分组下的所有属性：{}", relationEntities);

        List<Long> attrIds = relationEntities.stream().map(AttrAttrgroupRelationEntity::getAttrId).toList();

        LambdaQueryWrapper<AttrEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AttrEntity::getCatalogId, catalogId)
                .eq(AttrEntity::getAttrType, ProductConstant.AttrEnum.TYPE_BASE.getCode())
                .notIn(!attrIds.isEmpty(), AttrEntity::getAttrId, attrIds);

        String key = (String) params.get("key");
        if (key != null && !key.isEmpty()) {
            wrapper.like(AttrEntity::getAttrName, key)
                    .or()
                    .like(AttrEntity::getAttrId, key);
        }

        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    @Override
    public List<Long> selectSearchAttrs(List<Long> attrIds) {
        return this.baseMapper.selectSearchAttrs(attrIds);
    }

    @Override
    public void deleteByIds(List<Long> list) {
        List<AttrEntity> attrEntities = baseMapper.selectByIds(list);
        List<String> objectNames = attrEntities.stream().map(AttrEntity::getIcon).toList();
        R r = thirdPartyFeignService.deleteFile(objectNames);
        if (r.getCode() != 0) {
            throw new BaseException("删除失败" + r.getMsg());
        }
        this.removeByIds(list);
    }

}
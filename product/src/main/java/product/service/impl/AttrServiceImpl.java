package product.service.impl;

import ch.qos.logback.core.util.StringUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.constant.ProductConstant;
import common.utils.PageUtils;
import common.utils.Query;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import product.dao.AttrAttrgroupRelationDao;
import product.dao.AttrDao;
import product.dao.AttrGroupDao;
import product.dao.CategoryDao;
import product.entity.AttrAttrgroupRelationEntity;
import product.entity.AttrEntity;
import product.entity.AttrGroupEntity;
import product.entity.CategoryEntity;
import product.service.AttrService;
import product.service.CategoryService;
import product.vo.AttrRespVO;
import product.vo.AttrVO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Service("attrService")
public class AttrServiceImpl extends ServiceImpl<AttrDao, AttrEntity> implements AttrService {
    private final AttrAttrgroupRelationDao relationDao;

    private final AttrGroupDao attrGroupDao;

    private final CategoryDao categoryDao;

    private final CategoryService categoryService;

    public AttrServiceImpl(AttrAttrgroupRelationDao relationDao, AttrGroupDao attrGroupDao, CategoryDao categoryDao, CategoryService categoryService) {
        this.relationDao = relationDao;
        this.attrGroupDao = attrGroupDao;
        this.categoryDao = categoryDao;
        this.categoryService = categoryService;
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
            wrapper.eq(AttrEntity::getCatelogId, categoryId);
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
            if (first.isEmpty()) {
                return attrRespVO;
            }
            AttrAttrgroupRelationEntity relation = first.get();
            Optional<AttrGroupEntity> first1 = attrGroupEntities.stream().filter(attrGroup -> attrGroup.getAttrGroupId().equals(relation.getAttrGroupId())).findFirst();
            if (first1.isEmpty()) {
                return attrRespVO;
            }
            AttrGroupEntity attrGroup = first1.get();
            attrRespVO.setGroupName(attrGroup.getAttrGroupName());
            attrRespVO.setAttrGroupId(attrGroup.getAttrGroupId());


            Optional<CategoryEntity> first2 = categoryEntities.stream().filter(it -> it.getCatId().equals(attr.getCatelogId())).findFirst();
            if (first2.isEmpty()) {
                return attrRespVO;
            }
            CategoryEntity categoryEntity = first2.get();
            attrRespVO.setCatelogName(categoryEntity.getName());


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
            attrRespVO.setCatelogId(attrGroupEntity.getCatelogId());
        }

        Long catelogId = attrEntity.getCatelogId();
        attrRespVO.setCatelogPath(categoryService.findCatelogIds(catelogId));
        attrRespVO.setCatelogName(categoryDao.selectById(catelogId).getName());
        return attrRespVO;
    }

    @Override
    @Transactional
    public void updateAttr(AttrVO attr) {
        AttrEntity attrEntity = new AttrEntity();
        BeanUtils.copyProperties(attr, attrEntity);
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

        Long catelogId = attrGroupEntity.getCatelogId();

        List<AttrGroupEntity> attrGroupEntities = attrGroupDao.selectList(
                new LambdaQueryWrapper<>(AttrGroupEntity.class)
                        .eq(AttrGroupEntity::getCatelogId, catelogId)
        );

        List<Long> attrGroupIds = attrGroupEntities.stream().map(AttrGroupEntity::getAttrGroupId).toList();

        List<AttrAttrgroupRelationEntity> relationEntities = relationDao.selectList(
                new LambdaQueryWrapper<>(AttrAttrgroupRelationEntity.class)
                        .in(AttrAttrgroupRelationEntity::getAttrGroupId, attrGroupIds)
        );

        List<Long> attrIds = relationEntities.stream().map(AttrAttrgroupRelationEntity::getAttrId).toList();

        LambdaQueryWrapper<AttrEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AttrEntity::getCatelogId, catelogId)
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

}
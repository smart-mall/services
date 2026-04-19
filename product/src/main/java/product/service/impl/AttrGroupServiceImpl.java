package product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.utils.PageUtils;
import common.utils.Query;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import product.dao.AttrAttrgroupRelationDao;
import product.dao.AttrGroupDao;
import product.entity.AttrAttrgroupRelationEntity;
import product.entity.AttrEntity;
import product.entity.AttrGroupEntity;
import product.service.AttrGroupService;
import product.service.AttrService;
import product.vo.AttrGroupRelationVO;
import product.vo.AttrGroupWithAttrsVO;
import product.vo.SpuItemAttrGroupVo;

import java.util.List;
import java.util.Map;


@Service("attrGroupService")
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupDao, AttrGroupEntity> implements AttrGroupService {
    private final AttrAttrgroupRelationDao relationDao;
    private final AttrService attrService;

    public AttrGroupServiceImpl(AttrAttrgroupRelationDao relationDao, AttrService attrService) {
        this.relationDao = relationDao;
        this.attrService = attrService;
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
        LambdaQueryWrapper<AttrGroupEntity> lambdaQueryWrapper = new LambdaQueryWrapper<>();

        if (categoryId != null && categoryId != 0) {
            lambdaQueryWrapper.eq(AttrGroupEntity::getCatelogId, categoryId);
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
        return  new PageUtils(page);
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
    public List<AttrGroupWithAttrsVO> getAttrGroupWithAttrs(Long catelogId) {
        List<AttrGroupEntity> attrGroupEntities = baseMapper.selectList(new LambdaQueryWrapper<>(AttrGroupEntity.class).eq(AttrGroupEntity::getCatelogId, catelogId));

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
}
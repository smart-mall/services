package product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.utils.PageUtils;
import common.utils.Query;
import org.springframework.stereotype.Service;
import product.dao.AttrAttrgroupRelationDao;
import product.dao.AttrGroupDao;
import product.entity.AttrAttrgroupRelationEntity;
import product.entity.AttrGroupEntity;
import product.service.AttrGroupService;
import product.vo.AttrGroupRelationVO;

import java.util.Map;


@Service("attrGroupService")
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupDao, AttrGroupEntity> implements AttrGroupService {
    private final AttrAttrgroupRelationDao relationDao;

    public AttrGroupServiceImpl(AttrAttrgroupRelationDao relationDao) {
        this.relationDao = relationDao;
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
}
package product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import product.dao.AttrAttrgroupRelationDao;
import product.entity.AttrAttrgroupRelationEntity;
import product.service.AttrAttrgroupRelationService;
import product.vo.AttrGroupRelationVO;

import java.util.List;
import java.util.Map;


import common.query.PageQuery;
@Service("attrAttrgroupRelationService")
public class AttrAttrgroupRelationServiceImpl extends ServiceImpl<AttrAttrgroupRelationDao, AttrAttrgroupRelationEntity> implements AttrAttrgroupRelationService {

    @Override
    public PageVO<AttrAttrgroupRelationEntity> queryPage(PageQuery query) {
        IPage<AttrAttrgroupRelationEntity> page = this.page(
                query.toPage(),
                new QueryWrapper<>()
        );

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

    @Override
    public void addRelation(List<AttrGroupRelationVO> vos) {
        List<AttrAttrgroupRelationEntity> list = vos.stream().map(item -> {
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            BeanUtils.copyProperties(item, relationEntity);
            return relationEntity;
        }).toList();
        baseMapper.insert(list);

    }

}
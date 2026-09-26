package product.service.impl;

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
/**
 * 属性分组关联服务的默认实现，基于 MyBatis-Plus 的 {@code ServiceImpl} 读写
 * {@code pms_attr_attrgroup_relation}。
 *
 * <p>批量绑定用 Mapper 的批量插入一次落库，不逐条判重，因此同一对
 * {@code attrId} 与 {@code attrGroupId} 重复提交会留下多行。
 */
@Service("attrAttrgroupRelationService")
public class AttrAttrgroupRelationServiceImpl extends ServiceImpl<AttrAttrgroupRelationDao, AttrAttrgroupRelationEntity> implements AttrAttrgroupRelationService {

    /** {@inheritDoc} */
    @Override
    public PageVO<AttrAttrgroupRelationEntity> queryPage(PageQuery query) {
        IPage<AttrAttrgroupRelationEntity> page = this.page(query.toPage());

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

    /** {@inheritDoc} */
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
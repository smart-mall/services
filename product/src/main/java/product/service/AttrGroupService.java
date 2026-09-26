package product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import product.entity.AttrGroupEntity;
import product.vo.AttrGroupRelationVO;
import product.vo.AttrGroupRespVO;
import product.vo.AttrGroupWithAttrsVO;
import product.vo.SpuItemAttrGroupVo;

import java.util.List;
import java.util.Map;

import common.query.KeyPageQuery;
import common.query.PageQuery;
/**
 * 属性分组
 */
public interface AttrGroupService extends IService<AttrGroupEntity> {

    PageVO<AttrGroupEntity> queryPage(PageQuery query);

    PageVO<AttrGroupRespVO> queryPage(KeyPageQuery query, Long categoryId);

    void deleteRelation(AttrGroupRelationVO[] vos);

    List<AttrGroupWithAttrsVO> getAttrGroupWithAttrs(Long catalogId);

    List<SpuItemAttrGroupVo> getAttrGroupWithAttrsBySpuId(Long spuId, Long catalogId);

    void deleteByIds(List<Long> list);

    void updateDetail(AttrGroupEntity attrGroup);
}


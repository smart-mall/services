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

/**
 * 属性分组
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 09:58:33
 */
public interface AttrGroupService extends IService<AttrGroupEntity> {

    PageVO<AttrGroupEntity> queryPage(Map<String, Object> params);

    PageVO<AttrGroupRespVO> queryPage(Map<String, Object> params, Long categoryId);

    void deleteRelation(AttrGroupRelationVO[] vos);

    List<AttrGroupWithAttrsVO> getAttrGroupWithAttrs(Long catalogId);

    List<SpuItemAttrGroupVo> getAttrGroupWithAttrsBySpuId(Long spuId, Long catalogId);

    void deleteByIds(List<Long> list);

    void updateDetail(AttrGroupEntity attrGroup);
}


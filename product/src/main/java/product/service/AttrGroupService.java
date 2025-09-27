package product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.utils.PageUtils;
import product.entity.AttrGroupEntity;
import product.vo.AttrGroupRelationVO;
import product.vo.AttrGroupWithAttrsVO;

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

    PageUtils queryPage(Map<String, Object> params);

    PageUtils queryPage(Map<String, Object> params, Long categoryId);

    void deleteRelation(AttrGroupRelationVO[] vos);

    List<AttrGroupWithAttrsVO> getAttrGroupWithAttrs(Long catelogId);
}


package product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import product.entity.AttrAttrgroupRelationEntity;
import product.vo.AttrGroupRelationVO;

import java.util.List;
import java.util.Map;

import common.query.PageQuery;
/**
 * 属性&属性分组关联
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 09:58:33
 */
public interface AttrAttrgroupRelationService extends IService<AttrAttrgroupRelationEntity> {

    PageVO<AttrAttrgroupRelationEntity> queryPage(PageQuery query);

    void addRelation(List<AttrGroupRelationVO> vos);
}


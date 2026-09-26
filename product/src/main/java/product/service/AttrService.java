package product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import product.entity.AttrEntity;
import product.vo.AttrRespVO;
import product.vo.AttrVO;

import java.util.List;
import java.util.Map;

/**
 * 商品属性
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 09:58:33
 */
public interface AttrService extends IService<AttrEntity> {

    PageVO<AttrEntity> queryPage(Map<String, Object> params);

    void saveAttr(AttrVO attr);

    PageVO<AttrRespVO> queryBaseAttrPage(Map<String, Object> params, Long categoryId, String attrType);

    AttrRespVO getAttrInfo(Long attrId);

    void updateAttr(AttrVO attr);

    List<AttrEntity> getRelationAttr(Long attrGroupId);

    PageVO<AttrEntity> getNoRelationAttr(Long attrGroupId, Map<String, Object> params);

    List<Long> selectSearchAttrs(List<Long> attrIds);

    void deleteByIds(List<Long> list);
}


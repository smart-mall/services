package product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import product.entity.CategoryEntity;
import product.vo.CategoryVo;

import java.util.List;
import java.util.Map;

import common.query.PageQuery;
/**
 * 商品三级分类
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 09:58:33
 */
public interface CategoryService extends IService<CategoryEntity> {

    PageVO<CategoryEntity> queryPage(PageQuery query);

    List<CategoryEntity> listWithTree();

    void removeMenuByIds(List<Long> list);

    List<Long> findcatalogIds(Long catId);

    void updateDetail(CategoryEntity category);

    /**
     * 前台首页/全局导航使用的完整三级分类树，一级分类已按 sort 升序排列，子分类同样有序。
     *
     * @return 一级分类列表，每个节点通过 children 嵌套二级、三级分类
     */
    List<CategoryVo> getCatalogTree();
}


package product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.utils.PageUtils;
import product.entity.CategoryEntity;

import java.util.Map;

/**
 * 商品三级分类
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 09:58:33
 */
public interface CategoryService extends IService<CategoryEntity> {

    PageUtils queryPage(Map<String, Object> params);
}


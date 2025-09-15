package product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.utils.PageUtils;
import product.entity.AttrEntity;

import java.util.Map;

/**
 * 商品属性
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 09:58:33
 */
public interface AttrService extends IService<AttrEntity> {

    PageUtils queryPage(Map<String, Object> params);
}


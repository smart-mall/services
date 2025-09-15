package product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.utils.PageUtils;
import product.entity.SpuImagesEntity;

import java.util.Map;

/**
 * spu图片
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 09:58:33
 */
public interface SpuImagesService extends IService<SpuImagesEntity> {

    PageUtils queryPage(Map<String, Object> params);
}


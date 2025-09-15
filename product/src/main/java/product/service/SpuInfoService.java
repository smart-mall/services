package product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.utils.PageUtils;
import product.entity.SpuInfoEntity;

import java.util.Map;

/**
 * spu信息
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 09:58:33
 */
public interface SpuInfoService extends IService<SpuInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);
}


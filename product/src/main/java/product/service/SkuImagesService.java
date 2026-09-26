package product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import product.entity.SkuImagesEntity;

import java.util.List;
import java.util.Map;

import common.query.PageQuery;
/**
 * sku图片
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 09:58:33
 */
public interface SkuImagesService extends IService<SkuImagesEntity> {

    PageVO<SkuImagesEntity> queryPage(PageQuery query);

    List<SkuImagesEntity> getImagesBySkuId(Long skuId);
}


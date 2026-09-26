package product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import product.entity.SpuImagesEntity;

import java.util.Map;

import common.query.PageQuery;
/**
 * spu图片
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 09:58:33
 */
public interface SpuImagesService extends IService<SpuImagesEntity> {

    PageVO<SpuImagesEntity> queryPage(PageQuery query);
}


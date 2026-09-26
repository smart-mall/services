package product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import product.entity.SpuImagesEntity;

import java.util.Map;

import common.query.PageQuery;
/**
 * spu图片
 */
public interface SpuImagesService extends IService<SpuImagesEntity> {

    PageVO<SpuImagesEntity> queryPage(PageQuery query);
}


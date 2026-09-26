package product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import product.entity.SpuInfoDescEntity;

import java.util.Map;

import common.query.PageQuery;
/**
 * spu信息介绍
 */
public interface SpuInfoDescService extends IService<SpuInfoDescEntity> {

    PageVO<SpuInfoDescEntity> queryPage(PageQuery query);
}


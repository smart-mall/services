package product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import product.entity.SpuInfoDescEntity;

import java.util.Map;

import common.query.PageQuery;
/**
 * spu信息介绍
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 09:58:33
 */
public interface SpuInfoDescService extends IService<SpuInfoDescEntity> {

    PageVO<SpuInfoDescEntity> queryPage(PageQuery query);
}


package product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import product.entity.SpuImagesEntity;


import common.query.PageQuery;
/**
 * spu 图片服务：维护 spu 图集。
 */
public interface SpuImagesService extends IService<SpuImagesEntity> {

    /**
     * 分页查询全部 spu 图片，不带筛选条件。
     *
     * @param query 分页参数，{@code page} 为页码、{@code limit} 为每页条数，不能为 {@code null}
     * @return 分页结果，{@code rows} 为图片列表
     */
    PageVO<SpuImagesEntity> queryPage(PageQuery query);
}


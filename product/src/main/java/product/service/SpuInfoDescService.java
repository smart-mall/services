package product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import product.entity.SpuInfoDescEntity;


import common.query.PageQuery;
/**
 * spu 介绍服务：维护 spu 的商品描述图。
 *
 * <p>描述图地址按逗号拼接存在一列里，取用时要自行拆分。
 */
public interface SpuInfoDescService extends IService<SpuInfoDescEntity> {

    /**
     * 分页查询全部 spu 介绍，不带筛选条件。
     *
     * @param query 分页参数，{@code page} 为页码、{@code limit} 为每页条数，不能为 {@code null}
     * @return 分页结果，{@code rows} 为介绍列表
     */
    PageVO<SpuInfoDescEntity> queryPage(PageQuery query);
}


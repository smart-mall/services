package product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import product.entity.SpuCommentEntity;


import common.query.PageQuery;
/** 商品评价服务：维护 spu 的用户评价。 */
public interface SpuCommentService extends IService<SpuCommentEntity> {

    /**
     * 分页查询全部商品评价，不带筛选条件。
     *
     * @param query 分页参数，{@code page} 为页码、{@code limit} 为每页条数，不能为 {@code null}
     * @return 分页结果，{@code rows} 为评价列表
     */
    PageVO<SpuCommentEntity> queryPage(PageQuery query);
}


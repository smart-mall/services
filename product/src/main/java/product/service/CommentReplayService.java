package product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import product.entity.CommentReplayEntity;


import common.query.PageQuery;
/** 商品评价回复服务：维护评价与其回复之间的对应关系。 */
public interface CommentReplayService extends IService<CommentReplayEntity> {

    /**
     * 分页查询全部评价回复关系，不带筛选条件。
     *
     * @param query 分页参数，{@code page} 为页码、{@code limit} 为每页条数，不能为 {@code null}
     * @return 分页结果，{@code rows} 为回复关系列表
     */
    PageVO<CommentReplayEntity> queryPage(PageQuery query);
}


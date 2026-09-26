package order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import order.entity.UndoLogEntity;


import common.query.PageQuery;
/**
 * 撤销日志服务：在通用 CRUD 之上提供后台列表的分页查询。
 */
public interface UndoLogService extends IService<UndoLogEntity> {

    /**
     * 分页查询撤销日志，供后台列表使用。
     *
     * <p>没有业务筛选条件，返回全部日志行。
     *
     * @param query 分页参数，不能为 {@code null}
     * @return 分页结果，{@code rows} 为当前页日志；当前页没有数据时为空列表
     */
    PageVO<UndoLogEntity> queryPage(PageQuery query);
}


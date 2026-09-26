package ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import ware.entity.UndoLogEntity;


import common.query.PageQuery;
/**
 * 回滚日志服务：对 {@code undo_log} 表做分页查询。
 *
 * <p>本服务内没有业务代码写这张表，增删改沿用 {@code IService} 的通用方法，接口只声明查询。
 */
public interface UndoLogService extends IService<UndoLogEntity> {

    /**
     * 分页查询回滚日志，不附加任何筛选条件。
     *
     * @param query 分页参数，不能为 {@code null}
     * @return 回滚日志分页数据；无数据时 {@code rows} 为空列表，{@code total} 为 0
     */
    PageVO<UndoLogEntity> queryPage(PageQuery query);
}


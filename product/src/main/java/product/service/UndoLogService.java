package product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import product.entity.UndoLogEntity;

import java.util.Map;

import common.query.PageQuery;
/**
 * 撤销日志服务：维护 Seata 的 {@code undo_log} 表，本接口只提供分页查询。
 */
public interface UndoLogService extends IService<UndoLogEntity> {

    /**
     * 分页查询全部撤销日志，不带筛选条件。
     *
     * @param query 分页参数，{@code page} 为页码、{@code limit} 为每页条数，不能为 {@code null}
     * @return 分页结果，{@code rows} 为撤销日志列表
     */
    PageVO<UndoLogEntity> queryPage(PageQuery query);
}


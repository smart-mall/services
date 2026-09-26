package order.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;

import order.dao.UndoLogDao;
import order.entity.UndoLogEntity;
import order.service.UndoLogService;


import common.query.PageQuery;
/**
 * 撤销日志服务实现，提供 Seata undo_log 的分页查询。
 *
 * <p>继承 MyBatis-Plus 的 {@code ServiceImpl}，无状态、线程安全。
 */
@Service("undoLogService")
public class UndoLogServiceImpl extends ServiceImpl<UndoLogDao, UndoLogEntity> implements UndoLogService {

    /** {@inheritDoc} */
    @Override
    public PageVO<UndoLogEntity> queryPage(PageQuery query) {
        IPage<UndoLogEntity> page = this.page(query.toPage());

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

}
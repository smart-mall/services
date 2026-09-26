package member.service.impl;

import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;

import member.dao.UndoLogDao;
import member.entity.UndoLogEntity;
import member.service.UndoLogService;


import common.query.PageQuery;
/**
 * undo_log 回滚日志的查询实现。
 *
 * <p>无状态、线程安全，只做分页透传，不修改数据。
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
package ware.service.impl;

import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;

import ware.dao.UndoLogDao;
import ware.entity.UndoLogEntity;
import ware.service.UndoLogService;


import common.query.PageQuery;
/**
 * 回滚日志服务的默认实现，基于 MyBatis-Plus 的 {@code ServiceImpl} 读写 {@code undo_log}。
 *
 * <p>本服务内没有业务代码写这张表，这里只提供分页查询。
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
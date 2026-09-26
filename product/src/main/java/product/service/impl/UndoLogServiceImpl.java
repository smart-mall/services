package product.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;
import org.springframework.stereotype.Service;
import product.dao.UndoLogDao;
import product.entity.UndoLogEntity;
import product.service.UndoLogService;



import common.query.PageQuery;
/**
 * 撤销日志服务的默认实现，基于 MyBatis-Plus 的 {@code ServiceImpl} 读写 {@code undo_log}。
 *
 * <p>本模块内没有业务逻辑读写这张表，本类只实现分页查询，增删改由继承的 {@code IService} 提供。
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
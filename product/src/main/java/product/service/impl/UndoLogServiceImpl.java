package product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;
import org.springframework.stereotype.Service;
import product.dao.UndoLogDao;
import product.entity.UndoLogEntity;
import product.service.UndoLogService;

import java.util.Map;


import common.query.PageQuery;
@Service("undoLogService")
public class UndoLogServiceImpl extends ServiceImpl<UndoLogDao, UndoLogEntity> implements UndoLogService {

    @Override
    public PageVO<UndoLogEntity> queryPage(PageQuery query) {
        IPage<UndoLogEntity> page = this.page(
                query.toPage(),
                new QueryWrapper<UndoLogEntity>()
        );

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

}
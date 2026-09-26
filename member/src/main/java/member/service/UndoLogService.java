package member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import member.entity.UndoLogEntity;

import java.util.Map;

import common.query.PageQuery;
public interface UndoLogService extends IService<UndoLogEntity> {

    PageVO<UndoLogEntity> queryPage(PageQuery query);
}


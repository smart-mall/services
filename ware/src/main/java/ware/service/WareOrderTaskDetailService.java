package ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import ware.entity.WareOrderTaskDetailEntity;

import java.util.Map;

import common.query.PageQuery;
/**
 * 库存工作单
 */
public interface WareOrderTaskDetailService extends IService<WareOrderTaskDetailEntity> {

    PageVO<WareOrderTaskDetailEntity> queryPage(PageQuery query);
}


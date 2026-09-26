package ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import ware.entity.WareOrderTaskEntity;

import java.util.Map;

import common.query.PageQuery;
/**
 * 库存工作单
 */
public interface WareOrderTaskService extends IService<WareOrderTaskEntity> {

    PageVO<WareOrderTaskEntity> queryPage(PageQuery query);

    WareOrderTaskEntity getOrderTaskByOrderSn(String orderSn);
}


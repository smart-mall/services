package order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import order.entity.RefundInfoEntity;

import java.util.Map;

import common.query.PageQuery;
/**
 * 退款信息
 */
public interface RefundInfoService extends IService<RefundInfoEntity> {

    PageVO<RefundInfoEntity> queryPage(PageQuery query);
}


package order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.utils.PageUtils;
import order.entity.RefundInfoEntity;

import java.util.Map;

/**
 * 退款信息
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 11:22:13
 */
public interface RefundInfoService extends IService<RefundInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);
}


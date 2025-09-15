package ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.utils.PageUtils;
import ware.entity.PurchaseDetailEntity;

import java.util.Map;

/**
 * 
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 11:20:17
 */
public interface PurchaseDetailService extends IService<PurchaseDetailEntity> {

    PageUtils queryPage(Map<String, Object> params);
}


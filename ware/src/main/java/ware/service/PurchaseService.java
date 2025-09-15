package ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.utils.PageUtils;
import ware.entity.PurchaseEntity;

import java.util.Map;

/**
 * 采购信息
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 11:20:17
 */
public interface PurchaseService extends IService<PurchaseEntity> {

    PageUtils queryPage(Map<String, Object> params);
}


package ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import ware.entity.PurchaseDetailEntity;

import java.util.List;
import java.util.Map;

/**
 * 
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 11:20:17
 */
public interface PurchaseDetailService extends IService<PurchaseDetailEntity> {

    PageVO<PurchaseDetailEntity> queryPage(Map<String, Object> params);

    void saveDetail(PurchaseDetailEntity detail);

    void updateDetail(PurchaseDetailEntity detail);

    void removeDetails(List<Long> ids);
}

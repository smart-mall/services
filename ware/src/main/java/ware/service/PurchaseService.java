package ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import ware.entity.PurchaseEntity;
import ware.vo.MergeVO;
import ware.vo.PurchaseAssignVO;
import ware.vo.PurchaseDoneVO;

import java.util.List;
import java.util.Map;

import ware.vo.PurchasePageQuery;
import common.query.PageQuery;
/**
 * 采购信息
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 11:20:17
 */
public interface PurchaseService extends IService<PurchaseEntity> {

    PageVO<PurchaseEntity> queryPage(PurchasePageQuery query);

    PageVO<PurchaseEntity> queryPageUnreceive(PageQuery query);

    void merge(MergeVO mergeVO);

    void assign(PurchaseAssignVO assignVO);

    void unassign(List<Long> itemIds);

    void removePurchase(List<Long> ids);

    /** 领取采购单。领取人是当前登录管理员，只有这张单分配的采购员能领 */
    void receive(Long currentAdminId, List<Long> ids);

    void done(PurchaseDoneVO purchaseDoneVO);
}


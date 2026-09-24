package ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.to.OrderTo;
import common.to.SkuDeleteBlockerTo;
import common.to.mq.StockLockedTo;
import common.utils.PageUtils;
import ware.entity.WareSkuEntity;
import ware.vo.SkuHasStockVo;
import ware.vo.WareSkuLockVo;

import java.util.List;
import java.util.Map;

/**
 * 商品库存
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 11:20:17
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void addStock(Long skuId, Long wareId, Integer skuNum);

    List<SkuHasStockVo> getSkusHasStock(List<Long> skuIds);

    boolean orderLockStock(WareSkuLockVo vo);

    void unlockStock(StockLockedTo to);

    void unlockStock(OrderTo orderTo);

    /**
     * 这些 sku 在仓库侧还有没有删不掉的东西：有量的库存行、没走完的采购需求。
     *
     * @return 每个有阻塞的 sku 一条；返回空集合表示都能删
     */
    List<SkuDeleteBlockerTo> canDelete(List<Long> skuIds);

    /**
     * 商品删除后清掉这些 sku 的库存行。只删零行（stock 和 stock_locked 都不为正），
     * 有量的一律不动。
     *
     * @return 实际删掉的行数
     */
    int deleteZeroStock(List<Long> skuIds);
}


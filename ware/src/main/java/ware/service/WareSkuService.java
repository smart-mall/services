package ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.to.OrderTo;
import common.to.SkuDeleteBlockerTo;
import common.to.mq.StockLockedTo;
import common.vo.PageVO;
import ware.entity.WareSkuEntity;
import ware.vo.SkuHasStockVo;
import ware.vo.WareSkuLockVo;

import java.util.List;
import java.util.Map;

import ware.vo.WareSkuPageQuery;
/**
 * 商品库存服务：库存增加、可售库存查询、订单锁定与解锁，以及商品删除前的仓库侧守卫。
 *
 * <p>库存行按 {@code skuId} 与 {@code wareId} 定位，{@code stock} 只由采购完成增加，
 * {@code stock_locked} 只由订单锁定与解锁增减。
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    /**
     * 分页查询商品库存，并补齐每行的仓库名。
     *
     * @param query 分页与筛选条件，不能为 {@code null}；{@code skuId}、{@code wareId} 为空时对应条件不参与过滤
     * @return 库存分页数据；无命中时 {@code rows} 为空列表。仓库已被删除的库存行 {@code wareName} 为 {@code null}
     */
    PageVO<WareSkuEntity> queryPage(WareSkuPageQuery query);

    /**
     * 增加指定 SKU 在指定仓库的库存，库存行不存在时先建一行 0 库存再累加。
     *
     * <p>实现方必须用 SQL 做增量（{@code stock = stock + #{skuNum}}）：先查出来在内存里加再写回，
     * 并发入库会互相覆盖，后写的把先写的增量丢掉。
     *
     * @param skuId SKU ID，不能为 {@code null}
     * @param wareId 仓库 ID，不能为 {@code null}
     * @param skuNum 入库数量，不能为 {@code null} 且必须大于 0
     * @throws common.exception.ValidationException 任一参数为 {@code null} 或数量不大于 0 时抛出
     */
    void addStock(Long skuId, Long wareId, Integer skuNum);

    /**
     * 查询这些 SKU 是否有可售库存，可售量按 {@code stock - stock_locked} 汇总。
     *
     * @param skuIds SKU ID 列表，不能为 {@code null}
     * @return 每个入参 SKU 一条结果，顺序与入参一致；可售量不大于 0 或查不到库存行时 {@code hasStock} 为 {@code false}
     */
    List<SkuHasStockVo> getSkusHasStock(List<Long> skuIds);

    /**
     * 锁定订单占用的库存：逐 SKU 找一个有货的仓库增加锁定库存，并记录库存工作单。
     *
     * <p>实现方必须保证在同一事务内完成；每个 SKU 只锁第一个锁成功的仓库，任一 SKU 在所有仓库都锁不上
     * 就抛异常回滚整单。锁定成功的每个 SKU 都会投递一条延迟消息，供订单取消时释放库存。
     *
     * @param vo 订单号与需要锁定的 SKU 明细，不能为 {@code null}
     * @return 全部 SKU 锁定成功时返回 {@code true}；失败以异常表达，不会返回 {@code false}
     * @throws common.exception.NoStockException 某个 SKU 在所有仓库都没有可锁定库存时抛出
     */
    boolean orderLockStock(WareSkuLockVo vo);

    /**
     * 按库存锁定事件释放库存：仅当订单不存在或已关闭时解锁。
     *
     * <p>以工作单明细的锁定状态做幂等：只处理仍为"已锁定"的明细，重复消费不会把库存多减一次；
     * 查不到明细时直接返回，视为无需解锁。订单服务查询失败时抛运行时异常，由监听器把消息放回队列重投。
     *
     * @param to 库存锁定事件，含工作单 ID 与这条消息对应的锁定明细，不能为 {@code null}
     */
    void unlockStock(StockLockedTo to);

    /**
     * 按订单关闭事件释放该订单占用的全部库存。
     *
     * <p>先按订单号回查工作单，再释放其中所有仍为"已锁定"的明细；同一事件重复投递不会重复解锁。
     *
     * @param orderTo 订单事件，只读取其中的订单号，不能为 {@code null}
     */
    void unlockStock(OrderTo orderTo);

    /**
     * 判断这些 SKU 在仓库侧还有没有删不掉的东西：有量的库存行、没走完的采购需求。
     *
     * <p>只读接口，不修改任何数据；入参为空或全为 {@code null} 时返回空集合。
     *
     * @param skuIds 待删除的 SKU ID 列表，允许为 {@code null}
     * @return 每个有阻塞的 SKU 一条；返回空集合表示都能删
     */
    List<SkuDeleteBlockerTo> canDelete(List<Long> skuIds);

    /**
     * 清掉这些 SKU 的零库存行：只删 {@code stock} 与 {@code stock_locked} 都为 {@code null} 或小于等于 0 的行，
     * 有量的一律不动。
     *
     * <p>删除条件与 {@link #canDelete(List)} 的拦截条件互为取反，两边不漏；入参为空或全为 {@code null} 时返回 0。
     *
     * @param skuIds 待清理的 SKU ID 列表，允许为 {@code null}
     * @return 实际删掉的行数；有量的行被跳过时不计入
     */
    int deleteZeroStock(List<Long> skuIds);
}


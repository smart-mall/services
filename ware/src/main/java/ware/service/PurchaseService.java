package ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import ware.entity.PurchaseEntity;
import ware.vo.MergeVO;
import ware.vo.PurchaseAssignVO;
import ware.vo.PurchaseDoneVO;

import java.util.List;

import ware.vo.PurchasePageQuery;
import common.query.PageQuery;
/**
 * 采购单服务：采购单的分页查询、需求单合并、采购员分配与领取、提交完成与删除。
 *
 * <p>采购单由合并采购需求单时自动生成，状态流转规则见 {@link ware.costant.PurchaseStatusEnum}；
 * 所有写操作都以服务端查出的状态为准，调用方传入的 {@code status} 不参与判断。
 */
public interface PurchaseService extends IService<PurchaseEntity> {

    /**
     * 分页查询采购单，并补齐每行的仓库名与当前状态允许的操作。
     *
     * @param query 分页与筛选条件，不能为 {@code null}；{@code key} 同时匹配采购单 ID 与采购员姓名，
     *              {@code status} 为空时不按状态过滤
     * @return 采购单分页数据；无命中时 {@code rows} 为空列表，{@code total} 为 0
     */
    PageVO<PurchaseEntity> queryPage(PurchasePageQuery query);

    /**
     * 分页查询还没被领取的采购单，即"新建"与"已分配"两种状态。
     *
     * @param query 分页参数，不能为 {@code null}
     * @return 采购单分页数据，每行带当前状态允许的操作；无命中时 {@code rows} 为空列表
     */
    PageVO<PurchaseEntity> queryPageUnreceive(PageQuery query);

    /**
     * 合并采购需求单：{@code purchaseId} 为空时新建一张采购单，否则并入指定采购单。
     *
     * <p>实现方必须保证在同一事务内完成建单或改单、更新明细归属并重算采购单的总金额与仓库；
     * 采购单必须还没被领取，明细必须还没并入任何单，且本批明细与目标单的现有明细同属一个仓库。
     *
     * @param mergeVO 需求单 ID 列表与目标采购单，不能为 {@code null}；{@code items} 为空或全为
     *                {@code null} 时按参数校验失败处理
     * @throws common.exception.ValidationException 需求单列表为空，或优先级小于 1 时抛出
     * @throws common.exception.BaseException 明细不存在、明细状态不允许合并、仓库不一致、采购单不存在或已被领取时抛出
     */
    void merge(MergeVO mergeVO);

    /**
     * 给采购单分配采购人员，并把状态从"新建"推进到"已分配"。
     *
     * <p>只写采购员相关的字段，采购单必须还没被领取；已领取的单中途换人会让"谁在采购"无法追溯。
     *
     * @param assignVO 采购单 ID 与采购员信息，不能为 {@code null}；{@code assigneeId} 不能为
     *                 {@code null}，{@code assigneeName} 不能为空白
     * @throws common.exception.ValidationException 采购单 ID 为空，或采购员 ID 为空、姓名为空白时抛出
     * @throws common.exception.BaseException 采购单不存在或已被领取时抛出
     */
    void assign(PurchaseAssignVO assignVO);

    /**
     * 取消分配：把采购需求单从所属采购单摘出来，退回"新建"状态。
     *
     * <p>实现方必须保证在同一事务内更新明细归属并重算受影响采购单的总金额与仓库；
     * 明细必须处于"已分配"且所属采购单还没被领取，采购员已在按单采购时不能把明细抽走。
     *
     * @param itemIds 采购需求单 ID 列表，不能为 {@code null}；空列表或全为 {@code null} 时按参数校验失败处理
     * @throws common.exception.ValidationException 需求单列表为空时抛出
     * @throws common.exception.BaseException 明细不存在、明细状态不允许取消分配或所属采购单已被领取时抛出
     */
    void unassign(List<Long> itemIds);

    /**
     * 批量删除采购单，并处理单下的明细。
     *
     * <p>实现方必须保证在同一事务内处理明细：还没开始采购的单，明细退回"新建"；已到终态的单，
     * 明细一并删除；"已领取"的单不允许删除，终态单下若还有没走完的明细也拒绝删除。
     * 删除只针对单据，不回滚已经入库的库存。
     *
     * @param ids 采购单 ID 列表，不能为 {@code null}；空列表或全为 {@code null} 时按参数校验失败处理
     * @throws common.exception.ValidationException 采购单列表为空时抛出
     * @throws common.exception.BaseException 采购单不存在、已被领取，或单下还有没走完的明细时抛出
     */
    void removePurchase(List<Long> ids);

    /**
     * 领取采购单：单从"已分配"推进到"已领取"，单下仍为"已分配"的明细一起进入"正在采购"。
     *
     * <p>领取人必须是这张单分配的采购员，调用方要传服务端解析出的管理员 ID，不能使用前端传入的身份；
     * 单必须处于"已分配"且单下至少有 1 条明细，否则拒绝。
     *
     * @param currentAdminId 当前登录管理员 ID，不能为 {@code null}
     * @param ids 要领取的采购单 ID 列表，不能为 {@code null}；空列表或全为 {@code null} 时按参数校验失败处理
     * @throws common.exception.ValidationException 采购单列表为空时抛出
     * @throws common.exception.BaseException 采购单不存在、状态不是"已分配"、领取人不是分配的采购员，或单下没有明细时抛出
     */
    void receive(Long currentAdminId, List<Long> ids);

    /**
     * 提交采购结果，完成采购单。
     *
     * <p>每条明细只接受"已完成"或"采购失败"两种结果：全部成功时单落到"已完成"，否则落到"有异常"，
     * 采购成功的明细按数量增加对应仓库的库存，失败的明细不入库。单必须处于"已领取"且明细属于这张单，
     * 重复提交会因单已到终态而失败。
     *
     * @param purchaseDoneVO 采购单 ID 与逐条明细的采购结果，不能为 {@code null}；{@code items} 不能为空，
     *                       每条结果的 {@code itemId} 不能为 {@code null} 且不能重复
     * @throws common.exception.ValidationException 采购单 ID 为空、明细列表为空或明细 ID 为空或重复时抛出
     * @throws common.exception.BaseException 采购单不存在、状态不是"已领取"、明细不属于这张单，或明细结果不是终态时抛出
     */
    void done(PurchaseDoneVO purchaseDoneVO);
}


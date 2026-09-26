package ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import ware.entity.PurchaseDetailEntity;

import java.util.List;

import ware.vo.PurchaseDetailPageQuery;
/**
 * 采购需求单服务：需求单的分页查询、新建、修改与删除。
 *
 * <p>状态与归属由服务端决定，调用方传入的 {@code status} 与 {@code purchaseId} 会被忽略；
 * 状态流转规则见 {@link ware.costant.PurchaseDetailEnum}。
 */
public interface PurchaseDetailService extends IService<PurchaseDetailEntity> {

    /**
     * 分页查询采购需求单，并补齐每行的仓库名、SKU 名称与当前状态允许的操作。
     *
     * <p>SKU 名称通过商品服务的 Feign 接口批量补齐，商品服务不可用时整页查询失败；关键字过滤在内存中对
     * 当前页做，所以 {@code total} 是数据库命中的总行数，命中关键字时可能大于 {@code rows} 的行数。
     *
     * @param query 分页与筛选条件，不能为 {@code null}；{@code key} 同时匹配 SKU 名称与 SKU ID，
     *              {@code status}、{@code wareId}、{@code purchaseId} 为空时对应条件不参与过滤
     * @return 需求单分页数据；无命中时 {@code rows} 为空列表
     * @throws common.exception.BaseException 商品服务返回失败码或不可用时抛出
     */
    PageVO<PurchaseDetailEntity> queryPage(PurchaseDetailPageQuery query);

    /**
     * 新建采购需求单，状态固定为"新建"、归属清空，入参里的 {@code id}、{@code status}、{@code purchaseId} 会被覆盖。
     *
     * <p>实现方必须校验字段完整性，并确认仓库与 SKU 都还存在；商品服务不可用时拒绝建单，
     * 否则会建出一条指向不存在商品的需求。
     *
     * @param detail 需求单内容，不能为 {@code null}；{@code skuId}、{@code wareId} 不能为 {@code null}，
     *               {@code skuNum} 必须大于 0，{@code skuPrice} 不能为 {@code null} 且不能为负
     * @throws common.exception.ValidationException 上述字段不满足要求，或 SKU 不存在时抛出
     * @throws common.exception.BaseException 仓库不存在，或商品服务不可用时抛出
     */
    void saveDetail(PurchaseDetailEntity detail);

    /**
     * 修改采购需求单，只在"新建"状态允许。
     *
     * <p>并入采购单之后采购员已经照着它在买了，这时改数量或仓库会出现"买 10 件、系统入库 100 件"；
     * 要改必须先取消分配退回"新建"。实现方同样会覆盖入参里的 {@code status} 与 {@code purchaseId}。
     *
     * @param detail 需求单内容，不能为 {@code null} 且必须带 {@code id}；其余字段要求与
     *               {@link #saveDetail(PurchaseDetailEntity)} 相同
     * @throws common.exception.ValidationException {@code id} 为空或字段不满足要求时抛出
     * @throws common.exception.BaseException 需求单不存在，或已并入采购单、状态不是"新建"时抛出
     */
    void updateDetail(PurchaseDetailEntity detail);

    /**
     * 批量删除采购需求单，只在"新建"状态允许。
     *
     * @param ids 需求单 ID 列表，不能为 {@code null}；空列表或全为 {@code null} 时按参数校验失败处理
     * @throws common.exception.ValidationException 需求单列表为空时抛出
     * @throws common.exception.BaseException 需求单不存在，或已并入采购单时抛出
     */
    void removeDetails(List<Long> ids);
}

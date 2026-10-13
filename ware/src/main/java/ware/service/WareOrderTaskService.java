package ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import ware.entity.WareOrderTaskEntity;


import common.query.KeyPageQuery;
/**
 * 库存工作单服务：记录订单锁定库存时生成的工作单。
 *
 * <p>工作单以订单号关联订单，其明细记录锁定了哪些 SKU、锁在哪个仓库；
 * 订单关闭或锁定消息到期时按订单号回查工作单以释放库存。
 */
public interface WareOrderTaskService extends IService<WareOrderTaskEntity> {

    /**
     * 分页查询库存工作单，附带每条工作单的明细。
     *
     * <p>{@code key} 同时匹配订单号与收货人，为空时不加过滤；结果按创建时间倒序。
     * 明细随列表一起返回，展开某一行不必再发请求，每条的 {@code wareName} 也已按仓库 ID 填好。</p>
     *
     * @param query 分页与关键字条件，不能为 {@code null}
     * @return 工作单分页数据；无数据时 {@code rows} 为空列表，{@code total} 为 0
     */
    PageVO<WareOrderTaskEntity> queryPage(KeyPageQuery query);

    /**
     * 按订单号查询库存工作单。
     *
     * @param orderSn 订单号，不能为 {@code null}
     * @return 该订单号对应的工作单；查不到时返回 {@code null}
     */
    WareOrderTaskEntity getOrderTaskByOrderSn(String orderSn);
}


package common.to.mq;

import lombok.Data;


/**
 * 库存锁定成功事件：ware 锁定 SKU 库存后发到 {@code stock.exchange}（路由键 {@code stock.locked}），
 * 延迟到期后由 ware 自己消费，判断订单是否已取消以决定解锁。
 */
@Data
public class StockLockedTo {

    /** 库存工作单 ID。 */
    private Long id;

    /** 这条锁定记录对应的工作单详情。 */
    private StockDetailTo detailTo;
}

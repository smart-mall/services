package common.to.mq;

import lombok.Data;


/**
 * 库存工作单详情：ware 锁定库存后随 {@link StockLockedTo} 发到 {@code stock.exchange}
 * （路由键 {@code stock.locked}），经延迟队列到期后由 ware 自己消费，据此判断该不该解锁。
 */
@Data
public class StockDetailTo {

    /** 库存工作单详情 ID，解锁时按它回查详情。 */
    private Long id;
    /** SKU ID。 */
    private Long skuId;
    /** SKU 名称。 */
    private String skuName;
    /** 锁定数量。 */
    private Integer skuNum;
    /** 所属库存工作单 ID。 */
    private Long taskId;

    /** 锁定所在仓库 ID。 */
    private Long wareId;

    /** 锁定状态：1 已锁定，2 已解锁。 */
    private Integer lockStatus;

}

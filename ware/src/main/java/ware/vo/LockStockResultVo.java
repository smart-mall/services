package ware.vo;

import lombok.Data;


/**
 * 库存锁定结果：单个 SKU 的锁定数量与是否锁定成功。
 *
 * <p>当前没有接口或 service 引用本类。
 */
@Data
public class LockStockResultVo {

    /** SKU ID。 */
    private Long skuId;

    /** 该 SKU 锁定的数量。 */
    private Integer num;

    /** 是否锁定成功。 */
    private Boolean locked;

}

package ware.vo;

import lombok.Data;


/**
 * 单条采购需求的采购结果。
 *
 * <p>字段与 {@link PurchaseDoneVO.PurchaseItemVO} 一致，当前没有接口或 service 引用本类。
 */
@Data
public class PurchaseItemDoneVo {

    /** 采购需求单 ID。 */
    private Long itemId;

    /** 采购结果状态，只接受已完成(3)或采购失败(4)。 */
    private Integer status;

    /** 采购失败原因。 */
    private String reason;

}

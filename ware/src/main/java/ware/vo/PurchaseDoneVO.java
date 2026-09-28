package ware.vo;

import lombok.Data;

import java.util.List;

/**
 * 采购单完成的入参，对应 {@code POST /ware/purchase/done}：采购员提交一张采购单下每条采购需求的采购结果。
 *
 * <p>明细必须都属于这张单；只要有一条是采购失败，整张单落到"有异常"，否则落到"已完成"。
 */
@Data
public class PurchaseDoneVO {
    /** 采购单 ID，必须是已领取状态的单。 */
    private Long id;

    /** 逐条采购需求的采购结果，不能为空。 */
    private List<PurchaseItemVO> items;

    /** 单条采购需求的采购结果。 */
    @Data
    public static class PurchaseItemVO {
        /** 采购需求单 ID，必须属于该采购单。 */
        private Long itemId;

        /** 采购结果状态，只接受已完成(3)或采购失败(4)，取值见 {@link ware.constants.PurchaseDetailEnum}。 */
        private Integer status;

        /** 采购失败原因，由采购员填写；完成流程只落 {@code status}，不回写该字段。 */
        private String reason;
    }
}

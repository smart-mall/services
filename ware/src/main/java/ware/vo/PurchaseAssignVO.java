package ware.vo;

import lombok.Data;

/**
 * 分配采购人员的入参，对应 {@code POST /ware/purchase/assign}：给一张还没被领取的采购单指定采购员。
 *
 * <p>只接收采购单 ID 与采购员信息，单状态由服务端按状态机推进、不从请求体接收，
 * 免得一次请求顺带把 {@code status} 改掉。
 */
@Data
public class PurchaseAssignVO {

    /** 采购单 ID，必须是还没被领取的单（新建或已分配）。 */
    private Long id;

    /** 采购员用户 ID，不能为空。 */
    private Long assigneeId;

    /** 采购员姓名，不能为空，落库后供列表按它模糊搜索。 */
    private String assigneeName;

    /** 采购员联系电话，可空。 */
    private String phone;
}

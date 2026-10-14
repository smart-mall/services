package order.vo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 后台审核售后申请的入参，两条链共用。
 *
 * <p>审核只有两个结果：<b>通过</b>或<b>拒绝</b>。通过的含义按类型分岔 —— 仅退款是"钱退回去、
 * 订单作废"，退货退款是"同意退，把退货地址发给会员、等他寄回"；拒绝则都要在
 * {@code handleNote} 里写明理由，会员要靠这句话决定是不是补齐材料再申请一次。
 *
 * <p>退货地址与收货人<b>不由后台每次手填</b>：项目只有一个仓库、地址是固定值，由服务端填进申请单。
 * 判据与"物流单号由服务端生成、管理员不填"同源 —— 让管理员手填等于采集一个没有来源的数据，
 * 每次填的值都可能不一样。
 */
@Getter
@Setter
public class AfterSaleAuditVo {

    /** 售后申请单主键。 */
    @NotNull(message = "售后申请不能为空")
    private Long id;

    /** 是否通过：{@code true} 通过，{@code false} 拒绝。 */
    @NotNull(message = "请选择通过或拒绝")
    private Boolean approve;

    /** 处理备注，拒绝时必填（要告诉会员为什么），通过时可为空。 */
    @Size(max = 500, message = "处理备注不能超过 500 字")
    private String handleNote;
}

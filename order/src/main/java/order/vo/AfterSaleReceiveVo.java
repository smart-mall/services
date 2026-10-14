package order.vo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 后台"确认收到退货"的入参。
 *
 * <p>收货人是当前操作人（取自 {@code X-Admin}），不由前端传：调用方传的"收货人"和它传的申请单 ID
 * 一样不可信，拿它跟自己比对等于自己跟自己比 —— 与发货、采购领取人是同一口径。
 */
@Getter
@Setter
public class AfterSaleReceiveVo {

    /** 售后申请单主键，必须是"退货中"的退货单。 */
    @NotNull(message = "售后申请不能为空")
    private Long id;

    /** 收货备注，例如"外包装完好、配件齐全"；可为空。 */
    @Size(max = 500, message = "收货备注不能超过 500 字")
    private String receiveNote;
}

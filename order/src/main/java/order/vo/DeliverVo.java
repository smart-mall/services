package order.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 发货请求体（后台管理端）。
 *
 * <p>只有两个字段：订单号定位订单，物流公司是发货必须留下、之后要展示给会员的信息。</p>
 *
 * <p><b>没有物流单号：</b>真实场景下它由承运商出库时返回，项目没有对接任何快递接口 ——
 * 让管理员手填，等于采集一个没有来源的数据，填进去的每个值都是编的。改由服务端生成。</p>
 *
 * <p><b>没有"发货人"：</b>真要做操作人留痕就从管理端凭证里取，与采购领取人同一个道理 ——
 * 前端传的"操作人"和前端传的"订单号"一样不可信，让它和后端比对等于自己跟自己比。</p>
 */
@Data
public class DeliverVo {

    /** 订单号，业务主键，非自增 ID。 */
    @NotBlank(message = "不能为空")
    private String orderSn;

    /**
     * 物流公司编码，取值见 {@link order.enums.DeliveryCompanyEnum}。
     *
     * <p>取值合法性由服务层按枚举校验，不在这里用注解表达：注解表达不出"合法取值是哪几个"，
     * 而那几个值恰恰是拒绝时该告诉调用方的信息。</p>
     */
    @NotBlank(message = "不能为空")
    private String deliveryCompany;

}

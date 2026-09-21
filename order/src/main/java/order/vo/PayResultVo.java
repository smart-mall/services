package order.vo;

import lombok.Data;

/**
 * 发起支付的返回。
 *
 * <p>两种支付方式给前端的东西形态完全不同，所以放在一个对象里而不是两个接口：</p>
 * <ul>
 *   <li><b>支付宝</b>：{@code alipayTemplate.pay()} 返回的是**一整段 HTML 表单**
 *       （{@code AlipayClient.pageExecute()} 的 body），浏览器显示它就会自动跳到收银台。
 *       前端拿到 {@code form} 后写进一个新窗口即可。原来的 {@code /aliPayOrder} 是直接
 *       {@code produces="text/html"} 把这段 HTML 吐给浏览器，那是整页跳转时代的做法。
 *       ⚠️ 字段名不能叫 {@code body} —— 它和 {@code PayVo.body}（商品描述）容易混。</li>
 *   <li><b>微信</b>：Native 支付返回一个二维码链接，前端把它渲染成二维码然后轮询订单状态。</li>
 * </ul>
 */
@Data
public class PayResultVo {

    /** 支付方式，取值见 {@link order.constant.PayConstant}：1 支付宝 / 2 微信 */
    private Integer payType;

    /** 支付宝：HTML 表单，非空时忽略 codeUrl */
    private String form;

    /** 微信：二维码内容，非空时忽略 form */
    private String codeUrl;

}

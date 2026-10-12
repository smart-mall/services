package order.vo;

import lombok.Data;

/**
 * 发起支付的返回。
 *
 * <p>各渠道给前端的东西形态不同，所以放在一个对象里而不是多个接口，每次只有其中一个字段有值：</p>
 * <ul>
 *   <li><b>支付宝</b>：{@code form} 是一整段 HTML 表单，前端拿到 {@code form} 后写进一个新窗口，
 *       浏览器显示它就会自动跳到收银台。
 *       ⚠️ 字段名不能叫 {@code body} —— 它和 {@code PayVo.body}（商品描述）容易混。</li>
 *   <li><b>微信</b>：{@code codeUrl} 是二维码链接，前端把它渲染成二维码然后轮询订单状态。</li>
 *   <li><b>测试支付</b>：{@code cashierUrl} 是模拟支付平台的收银台地址，已带上一次性票据，
 *       前端在新窗口打开它；支付完成后由平台把浏览器跳回商户的订单页。</li>
 * </ul>
 */
@Data
public class PayResultVo {

    /** 支付方式，取值见 {@link order.constant.PayConstant}：1 支付宝 / 2 微信 / 9 测试支付。 */
    private Integer payType;

    /** 支付宝：HTML 表单；其余渠道为 {@code null}。 */
    private String form;

    /** 微信：二维码内容；其余渠道为 {@code null}。 */
    private String codeUrl;

    /** 测试支付：收银台地址；其余渠道为 {@code null}。 */
    private String cashierUrl;

}

package order.vo;

import lombok.Data;

/**
 * 订单状态投影。
 *
 * <p>两个消费者，要求的字段完全不同：</p>
 * <ul>
 *   <li><b>ware 的内部调用</b>（{@code ware/feign/OrderFeignService → /order/order/status/{orderSn}}）
 *       只读 {@code status}，用来判断订单是否已取消以决定解不解锁库存。它反序列化到
 *       {@code ware.vo.OrderVo}（一个字段很全的类），缺的字段留 null 就行，所以瘦 VO 不会出问题。</li>
 *   <li><b>SPA 的支付状态轮询</b>（{@code /order/front/status/{orderSn}}）要 {@code statusText} 直接显示。</li>
 * </ul>
 *
 * <p>刻意不返回整个 {@code OrderEntity}：内部那条路径是被拦截器白名单放行的（Feign 从 MQ 监听线程
 * 发起、没有请求上下文，带不了 {@code X-Member-Claims}），也就是**免登录可访问**。
 * 返回整单会连带把收货人姓名、电话、详细地址一起暴露出去。</p>
 */
@Data
public class OrderStatusVo {

    private String orderSn;

    /** 取值见 {@link order.enume.OrderStatusEnum} */
    private Integer status;

    /** 状态文案，如"待付款" */
    private String statusText;

}

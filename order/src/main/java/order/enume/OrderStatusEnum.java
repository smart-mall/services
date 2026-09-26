package order.enume;

/**
 * 订单状态枚举，{@code code} 落库到 {@code oms_order.status}，{@code msg} 作为状态文案返回前端。
 *
 * <p>状态取值的权威定义在本枚举：其它位置若出现与之不一致的取值说明，一律以枚举项为准。
 */

public enum OrderStatusEnum {
    /** 待付款：订单创建后写入的初始状态，只有该状态允许发起支付或取消，取值 0。 */
    CREATE_NEW(0,"待付款"),
    /** 已付款：收到支付成功回调后置入，取值 1。 */
    PAYED(1,"已付款"),
    /** 已发货：订单已出库，取值 2。 */
    SENDED(2,"已发货"),
    /** 已完成：交易结束，取值 3。 */
    RECIEVED(3,"已完成"),
    /** 已取消：取消订单或超时关单时置入，取值 4。 */
    CANCLED(4,"已取消"),
    /** 售后中：订单进入退货退款流程，取值 5。 */
    SERVICING(5,"售后中"),
    /** 售后完成：退货退款流程已结束，取值 6。 */
    SERVICED(6,"售后完成");
    /** 状态码，落库到 {@code oms_order.status}。 */
    private Integer code;
    /** 状态文案。 */
    private String msg;

    OrderStatusEnum(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}

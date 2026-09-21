package order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.to.mq.SeckillOrderTo;
import common.utils.PageUtils;
import order.entity.OrderEntity;
import order.vo.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 订单
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 11:22:13
 */
public interface OrderService extends IService<OrderEntity> {

    /** 后台订单列表（renren 的 /order/order/list 用），不做会员过滤 */
    PageUtils queryPage(Map<String, Object> params);

    /** 结算页数据：收货地址、已勾选商品、库存、积分、防重令牌、金额 */
    OrderConfirmVo confirmOrder();

    /**
     * 计算指定收货地址的运费。
     *
     * @throws common.exception.BaseException 地址不属于当前登录会员时抛 {@code ADDRESS_NOT_FOUND}
     */
    FareVo getFare(Long addrId);

    /**
     * 提交订单。
     *
     * @throws common.exception.BaseException 令牌失效 {@code ORDER_TOKEN_INVALID}、
     *         价格变动 {@code ORDER_PRICE_CHANGED}、库存不足 {@code NO_STOCK_EXCEPTION}
     */
    SubmitOrderResponseVo submitOrder(OrderSubmitVo vo);

    /** 我的订单分页。params 支持 pageNum / pageSize / status */
    PageUtils queryMemberOrders(Map<String, Object> params);

    /**
     * 订单详情（含订单项）。
     *
     * @throws common.exception.BaseException 订单不存在或不属于当前会员时抛 {@code ORDER_NOT_FOUND}
     */
    OrderEntity getOrderDetail(String orderSn);

    OrderEntity getOrderByOrderSn(String orderSn);

    /**
     * 发起支付。
     *
     * @throws common.exception.BaseException 订单不存在 {@code ORDER_NOT_FOUND}、
     *         状态不是待付款 {@code ORDER_STATUS_INVALID}
     */
    PayResultVo payOrder(String orderSn, Integer payType);

    /**
     * 查订单状态。<b>内部接口</b>：ware 在释放库存前用它判断订单是否已取消。
     *
     * <p>⚠️ 订单不存在时返回 {@code null}（HTTP 200 + code 0 + data null），
     * <b>不能改成抛异常</b> —— 这是 ware 的契约，见实现里的注释。</p>
     */
    OrderStatusVo getOrderStatus(String orderSn);

    /**
     * 查自己的订单状态（SPA 的扫码页轮询用）。
     *
     * @throws common.exception.BaseException 订单不存在或不属于当前会员时抛 {@code ORDER_NOT_FOUND}
     */
    OrderStatusVo getMyOrderStatus(String orderSn);

    /**
     * 取消未支付的订单，并通知仓库释放库存。
     *
     * @throws common.exception.BaseException 订单不存在 {@code ORDER_NOT_FOUND}、
     *         状态不是待付款 {@code ORDER_STATUS_INVALID}
     */
    void cancelOrder(String orderSn);

    /** 关闭超时未支付的订单（由 MQ 延迟消息触发） */
    void closeOrder(OrderEntity orderEntity);

    @Transactional(rollbackFor = Exception.class)
    String handlePayResult(PayAsyncVo asyncVo);

    String asyncNotify(String notifyData);

    void createSeckillOrder(SeckillOrderTo orderTo);
}

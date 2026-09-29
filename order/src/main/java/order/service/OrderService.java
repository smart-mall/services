package order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.to.mq.SeckillOrderTo;
import common.vo.PageVO;
import common.vo.MemberResponseVo;
import order.entity.OrderEntity;
import order.vo.*;
import org.springframework.transaction.annotation.Transactional;


import common.query.PageQuery;
import order.vo.OrderPageQuery;
/**
 * 订单服务：结算页组装、提交下单、订单查询与支付结果处理。
 *
 * <p>订单与收货地址的归属校验（是不是当前会员的）由实现负责，调用方传入登录会员即可。
 */
public interface OrderService extends IService<OrderEntity> {

    /**
     * 分页查询全部订单，供后台列表使用。
     *
     * <p>不做会员过滤：任何会员的订单都会返回。
     *
     * @param query 分页参数，不能为 {@code null}
     * @return 分页结果，{@code rows} 为当前页订单；当前页没有数据时为空列表
     */
    PageVO<OrderEntity> queryPage(PageQuery query);

    /**
     * 组装结算页数据：收货地址、已勾选购物项、库存、积分、防重令牌、可用券与四个金额。
     *
     * <p>副作用是写一条防重令牌到 Redis（{@code order:token:<memberId>}，有效期 30 分钟），
     * 每次调用都覆盖上一条，提交订单时必须原样回传。
     *
     * <p>抵扣额由券服务按同一份购物车算出，前端切换券后必须重新调用本接口取新的应付金额 ——
     * 前端拿券面金额自己减会与提交时的校验算成两个数。
     *
     * @param user 当前登录会员，不能为 {@code null}，{@code id} 不能为 {@code null}
     * @param couponHistoryId 选中的优惠券领取记录 ID；不用券时传 {@code null}。
     *                        传了但券不可用时抛出，不静默按不用券处理 —— 那样会员会以为自己用上了
     * @return 结算页数据；没有收货地址时 {@code addresses} 为空列表、{@code defaultAddrId} 为 {@code null}
     * @throws common.exception.BaseException 远程加载地址或购物车失败、购物项缺少价格或数量、
     *         运费算不出来时抛出，走兜底码 {@code UNKNOWN_EXCEPTION}；
     *         指定的券不可用时转发券服务的错误码
     */
    OrderConfirmVo confirmOrder(MemberResponseVo user, Long couponHistoryId);

    /**
     * 按当前购物车与指定收货地址重算结算金额。
     *
     * <p>商品清单取购物车已勾选项而不是前端传的参数，与提交订单取的是同一份数据；
     * 先确认地址属于该会员再计费，否则会拿别人的地址算运费。
     *
     * <p>影响价格的参数变了就该走本方法重算：地址换了重算运费，券换了重算优惠。
     * 选中券的抵扣额一并并进 {@code payAmount}，换地址只覆盖运费、不丢券的效果 ——
     * 否则前端换完地址拿到的应付金额会退回原价，提交时被判成价格变动。
     *
     * @param user   当前登录会员，不能为 {@code null}
     * @param addrId 收货地址 id，不能为 {@code null}，且必须是该会员的地址
     * @param couponHistoryId 选中的优惠券领取记录 ID；不用券时传 {@code null}
     * @return 四个金额与两份按商品的明细；购物车没有勾选项时运费为 0
     * @throws common.exception.BaseException 地址不属于该会员时抛 {@code ADDRESS_NOT_FOUND}；
     *         地址没有行政区划编码、ware 算不出运费时抛 {@code ORDER_FARE_FAILED}
     *         或原样转发 ware 的错误码；指定的券不可用时转发券服务的错误码
     */
    FareVo getFare(MemberResponseVo user, Long addrId, Long couponHistoryId);

    /**
     * 提交订单：校验地址与防重令牌、重算价格、落库、锁定库存，并清空购物车。
     *
     * <p>防重令牌是一次性的：校验通过即从 Redis 删除，同一令牌重复提交只有第一次会成功。
     * 副作用为写入订单与订单项、锁定 ware 库存、发送订单创建消息、删除该会员的购物车。
     *
     * @param user 当前登录会员，不能为 {@code null}
     * @param vo   提交参数，不能为 {@code null}；{@code addrId}、{@code orderToken}、
     *             {@code payPrice} 均不能为空
     * @return 提交结果，含落库后的订单；应付金额以服务端重算结果为准，不取前端回传值
     * @throws common.exception.BaseException 地址不属于该会员 {@code ADDRESS_NOT_FOUND}、令牌失效
     *         {@code ORDER_TOKEN_INVALID}、价格与确认页不一致 {@code ORDER_PRICE_CHANGED}、
     *         库存不足 {@code NO_STOCK_EXCEPTION}；购物车已清空或商品信息缺失时走兜底码
     */
    SubmitOrderResponseVo submitOrder(MemberResponseVo user, OrderSubmitVo vo);

    /**
     * 分页查询当前会员自己的订单，附带订单项与状态文案。
     *
     * <p>已删除的订单不返回，结果按创建时间倒序。
     *
     * @param user  当前登录会员，不能为 {@code null}
     * @param query 分页与筛选参数，不能为 {@code null}；{@code status} 为 {@code null} 时不按状态过滤
     * @return 分页结果，每行的 {@code orderItemEntityList} 与 {@code statusText} 已填好；
     *         当前页没有数据时 {@code rows} 为空列表
     */
    PageVO<OrderEntity> queryMemberOrders(MemberResponseVo user, OrderPageQuery query);

    /**
     * 查询订单详情，附带订单项与状态文案。
     *
     * @param user    当前登录会员，不能为 {@code null}
     * @param orderSn 订单号，不能为 {@code null}
     * @return 订单详情，{@code orderItemEntityList} 与 {@code statusText} 已填好
     * @throws common.exception.BaseException 订单不存在或不属于该会员时抛 {@code ORDER_NOT_FOUND}
     */
    OrderEntity getOrderDetail(MemberResponseVo user, String orderSn);

    /**
     * 按订单号查询订单，不做归属校验。
     *
     * <p>面向会员的接口不能用它：它会把别人的订单也返回，请改用 {@code getOrderDetail}。
     *
     * @param orderSn 订单号，不能为 {@code null}
     * @return 订单；订单号不存在时返回 {@code null}
     */
    OrderEntity getOrderByOrderSn(String orderSn);

    /**
     * 发起支付，返回拉起收银台所需的数据。
     *
     * <p>只有待付款的订单能发起支付。
     *
     * @param user    当前登录会员，不能为 {@code null}
     * @param orderSn 订单号，不能为 {@code null}
     * @param payType 支付方式，取值见 {@code order.constant.PayConstant}：1 支付宝、2 微信；
     *                其它值按参数格式校验失败处理
     * @return 支付宝时 {@code form} 有值，微信时 {@code codeUrl} 有值，两者不会同时为空
     * @throws common.exception.BaseException 订单不存在或不属于该会员 {@code ORDER_NOT_FOUND}、
     *         状态不是待付款 {@code ORDER_STATUS_INVALID}；支付渠道调用失败时走兜底码
     */
    PayResultVo payOrder(MemberResponseVo user, String orderSn, Integer payType);

    /**
     * 按订单号查询订单状态，供 ware 在释放库存前判断订单是否已取消。
     *
     * <p>内部接口，订单不存在时返回 {@code null} 而不是抛异常 —— 这是 ware 的契约：
     * 它靠 {@code data} 为 {@code null} 判定必须解锁库存，报错会让它重投消息、库存永远解不掉。
     *
     * @param orderSn 订单号，不能为 {@code null}
     * @return 订单状态；订单不存在时返回 {@code null}
     */
    OrderStatusVo getOrderStatus(String orderSn);

    /**
     * 查询当前会员自己的订单状态，供扫码支付页轮询。
     *
     * @param user    当前登录会员，不能为 {@code null}
     * @param orderSn 订单号，不能为 {@code null}
     * @return 订单状态，含状态码与状态文案
     * @throws common.exception.BaseException 订单不存在或不属于该会员时抛 {@code ORDER_NOT_FOUND}
     */
    OrderStatusVo getMyOrderStatus(MemberResponseVo user, String orderSn);

    /**
     * 取消当前会员未支付的订单，并通知仓库释放已锁定的库存。
     *
     * <p>副作用：订单状态置为已取消，并发送库存释放消息；消息发送失败只记日志，不回滚状态。
     *
     * @param user    当前登录会员，不能为 {@code null}
     * @param orderSn 订单号，不能为 {@code null}
     * @throws common.exception.BaseException 订单不存在或不属于该会员 {@code ORDER_NOT_FOUND}、
     *         状态不是待付款 {@code ORDER_STATUS_INVALID}
     */
    void cancelOrder(MemberResponseVo user, String orderSn);

    /**
     * 关闭超时未支付的订单，由 MQ 延迟消息触发。
     *
     * <p>幂等：订单不存在或已不是待付款状态时直接返回，不报错；消费失败的消息会重投，
     * 因此必须容忍重复调用。
     *
     * @param orderEntity 延迟消息携带的订单，只读取 {@code orderSn}；不能为 {@code null}
     */
    void closeOrder(OrderEntity orderEntity);

    /**
     * 处理支付宝的异步支付通知：落一条交易流水，并在支付成功时把订单置为已付款。
     *
     * <p>非支付成功状态只落流水，不改订单状态。
     *
     * @param asyncVo 支付宝通知参数，不能为 {@code null}；{@code out_trade_no} 为商户订单号
     * @return 固定返回字面量 {@code success}，支付宝收到该应答后停止重发通知
     */
    @Transactional(rollbackFor = Exception.class)
    String handlePayResult(PayAsyncVo asyncVo);

    /**
     * 处理微信的异步支付通知：验签、校验订单状态，并把订单置为已付款。
     *
     * <p>订单查不到、或订单已付款或已取消时直接抛异常，微信会按失败重发通知。
     *
     * @param notifyData 微信通知的原始报文，不能为 {@code null}
     * @return 微信要求的 {@code SUCCESS} 应答 XML，原样回给微信即可
     */
    String asyncNotify(String notifyData);

    /**
     * 创建秒杀订单：落订单主表与订单项，收货人取该会员的默认地址。
     *
     * <p>非幂等：同一条消息重复消费会建出重复订单。
     *
     * @param orderTo 秒杀订单消息，不能为 {@code null}；{@code orderSn}、{@code memberId}、
     *                {@code skuId}、{@code seckillPrice}、{@code num} 均不能为空
     */
    void createSeckillOrder(SeckillOrderTo orderTo);
}

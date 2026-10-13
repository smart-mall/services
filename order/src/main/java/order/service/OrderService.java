package order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.to.CommentTargetVo;
import common.to.mq.SeckillOrderTo;
import common.vo.PageVO;
import common.vo.MemberResponseVo;
import order.entity.OrderEntity;
import order.vo.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


import order.vo.OrderPageQuery;
/**
 * 订单服务：结算页组装、提交下单、订单查询、支付结果处理、发货、关单、收货、删除与操作史留痕。
 *
 * <p>订单与收货地址的归属校验（是不是当前会员的）由实现负责，调用方传入登录会员即可。
 */
public interface OrderService extends IService<OrderEntity> {

    /**
     * 分页查询全部订单，供后台列表使用。
     *
     * <p>不做会员过滤：任何会员的订单都会返回。结果按下单时间倒序。{@code key} 同时匹配订单号、
     * 收货人与下单会员名，时间区间按下单时间比较，除分页外的字段为空即不过滤。</p>
     *
     * <p>每行的 {@code statusText} 已填好，{@code orderItemEntityList} 为空：列表不带订单项，
     * 需要订单项请调用 {@link #getOrderDetailForAdmin}。</p>
     *
     * @param query 查询条件，不能为 {@code null}
     * @return 分页结果，{@code rows} 为当前页订单；当前页没有数据时为空列表
     */
    PageVO<OrderEntity> queryPage(OrderPageQuery query);

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
     * 查询订单详情，供后台查看，不做会员归属校验。
     *
     * <p>与查自己订单的详情只差归属校验与装配内容：后台还要看操作流水与支付流水，
     * 会员端不看，因此这两项只出现在本方法的返回值里。</p>
     *
     * @param orderSn 订单号，不能为 {@code null}
     * @return 订单详情，{@code orderItemEntityList}、{@code statusText}、{@code operateHistoryList}
     *         与 {@code paymentInfoList} 已填好
     * @throws common.exception.BaseException 订单不存在时抛 {@code ORDER_NOT_FOUND}
     */
    OrderEntity getOrderDetailForAdmin(String orderSn);

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
     * @param payType 支付方式，取值见 {@code order.constant.PayConstant}：1 支付宝、2 微信、9 测试支付；
     *                其它值按参数格式校验失败处理
     * @return 支付宝与测试支付把收银台页面写在 {@code form}，微信把二维码内容写在 {@code codeUrl}，
     *         两者不会同时为空
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
     * <p>副作用：订单状态置为已取消，并以消息记录的形式通知仓库释放库存；消息记录与状态变更在同一事务，
     * 写不进去就整体回滚。订单在并发中被支付成功时，取消不会生效。
     *
     * @param user    当前登录会员，不能为 {@code null}
     * @param orderSn 订单号，不能为 {@code null}
     * @throws common.exception.BaseException 订单不存在或不属于该会员 {@code ORDER_NOT_FOUND}、
     *         状态不是待付款、或并发中被支付成功 {@code ORDER_STATUS_INVALID}
     */
    void cancelOrder(MemberResponseVo user, String orderSn);

    /**
     * 删除会员自己的订单：把订单标记为已删除，会员端列表按标记过滤掉这类订单，数据本身保留。
     *
     * <p>只允许删除已完成与已取消的订单：进行中的订单还要继续走支付、发货与售后，从会员视图里
     * 消失会让会员找不到这些入口。订单项、支付流水与库存工作单都不动，后台仍能查到，
     * 物理删除由 {@link #hardDeleteOrder} 在确认无人需要之后再做。</p>
     *
     * <p>重复删除同一订单会落到 {@code ORDER_STATUS_INVALID}，不会重复生效。</p>
     *
     * @param user 当前登录会员，不能为 {@code null}
     * @param orderSn 订单号，不能为 {@code null}
     * @throws common.exception.BaseException 订单不存在或不属于该会员 {@code ORDER_NOT_FOUND}、
     *         状态不是已完成或已取消、或已被删除 {@code ORDER_STATUS_INVALID}
     */
    void deleteOrder(MemberResponseVo user, String orderSn);

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
     * 后台关闭订单：把待付款的订单置为已取消，并释放库存、退回优惠券。
     *
     * <p>与超时关单共用同一条路径，生效条件也一样（{@code status} 为待付款）；差别在结果反馈：
     * 状态不支持时抛异常，而不是像消息消费那样静默跳过。</p>
     *
     * <p>副作用：订单状态置为已取消，并以消息记录的形式通知仓库释放库存；消息记录与状态变更在同一事务，
     * 写不进去就整体回滚。订单在并发中被支付成功时，关单不生效。</p>
     *
     * @param orderSn 订单号，不能为 {@code null}
     * @param operator 操作人（后台管理员用户名），取自网关注入的 {@code X-Admin} 头，写进操作史
     * @throws common.exception.BaseException 订单不存在 {@code ORDER_NOT_FOUND}、
     *         状态不是待付款或并发中被支付成功 {@code ORDER_STATUS_INVALID}
     */
    void closeOrderByAdmin(String orderSn, String operator);

    /**
     * 修改订单备注。
     *
     * <p>备注是给运营看的说明，不参与状态流转，因此任何状态的订单都能改；可写的字段只有备注一个，
     * 金额、收货信息与状态都不在可改范围内。</p>
     *
     * <p>改备注不改状态，但同样记一条操作史：事后要能回答"谁在什么时候把备注改了"。</p>
     *
     * @param orderSn 订单号，不能为 {@code null}
     * @param note 订单备注，长度不超过 500；传 {@code null} 或空串表示清空
     * @param operator 操作人（后台管理员用户名），取自网关注入的 {@code X-Admin} 头
     * @throws common.exception.BaseException 订单不存在时抛 {@code ORDER_NOT_FOUND}
     */
    void updateNote(String orderSn, String note, String operator);

    /**
     * 后台硬删订单：把订单及其订单项、支付流水、售后记录与库存工作单一并删除。
     *
     * <p>只允许删除被会员标记过删除的订单，准入条件写在 SQL 里。远程侧先由 ware 校验该订单的库存
     * 工作单没有仍处于"已锁定"的明细：那种情况说明订单的解锁还没跑完，删掉工单会让这批库存永远
     * 解不开，此时拒绝并转发 {@code STOCK_STILL_LOCKED}。</p>
     *
     * <p><b>刻意不动券记录</b>：会员的限领额度是按 {@code sms_coupon_history} 的条数算的，
     * 删掉已核销的记录等于把额度退还给会员，可以"领券 → 下单 → 删单"反复领取同一张券。</p>
     *
     * <p>远程侧（ware 的工单）排在本地删除之前，不做分布式事务：本地事务回滚时工单已经删掉，
     * 而订单仍在、状态只可能是已完成或已取消，重试一次即可收敛；反过来先删本地，远程失败会留下
     * 一条指向已删订单的工单，且订单查不到、没有重试的入口。</p>
     *
     * <p>物理删除不可逆：成功时记一条带操作人的 warn 日志；订单的操作流水随订单一起删掉，
     * 订单侧不留指向已删订单的记录。</p>
     *
     * @param orderSn 订单号，不能为 {@code null}
     * @param operator 操作人（后台管理员用户名），取自网关注入的 {@code X-Admin} 头，不能为 {@code null}
     * @throws common.exception.BaseException 订单不存在或没被标记过 {@code ORDER_NOT_FOUND}；
     *         库存工作单仍有已锁定明细时转发 ware 的 {@code STOCK_STILL_LOCKED}
     */
    void hardDeleteOrder(String orderSn, String operator);

    /**
     * 处理支付宝的异步支付通知：落一条交易流水，并在支付成功时把订单置为已付款。
     *
     * <p>非支付成功状态只落流水，不改订单状态。同一个交易号的流水只落一条，重复通知不落流水。
     *
     * <p>幂等：只有待付款的订单会被推进到已付款。重复通知、订单查不到、以及订单已被取消或关单，
     * 都只记日志并照常回 {@code success}；已取消的订单不会被改回已付款。
     *
     * @param asyncVo 支付宝通知参数，不能为 {@code null}；{@code out_trade_no} 为商户订单号
     * @return 固定返回字面量 {@code success}，支付宝收到该应答后停止重发通知
     */
    @Transactional(rollbackFor = Exception.class)
    String handlePayResult(PayAsyncVo asyncVo);

    /**
     * 处理微信的异步支付通知：验签、落交易流水，并把订单置为已付款。
     *
     * <p>订单查不到时抛异常，微信会按失败重发通知。订单已付款或已取消时不改任何数据，
     * 但照常返回 {@code SUCCESS} 应答 —— 这两种情况下重发通知不会有新的结果。
     *
     * <p>流水、券核销与订单状态在同一事务内写入，三者必须一起生效。
     *
     * @param notifyData 微信通知的原始报文，不能为 {@code null}
     * @return 微信要求的 {@code SUCCESS} 应答 XML，原样回给微信即可
     */
    @Transactional(rollbackFor = Exception.class)
    String asyncNotify(String notifyData);

    /**
     * 处理测试支付的确认通知：校验一次性票据，并在支付成功时把订单置为已付款。
     *
     * <p>面向浏览器而不是支付平台：校验的是发起支付时写入 Redis 的一次性票据，不做验签。票据用掉即失效，
     * 重复提交同一张票据不会重复落流水。
     *
     * <p>订单已不是待付款状态时不改任何数据，只抛出状态错误 —— 超时关单之后到达的确认走的就是这一分支。
     *
     * @param orderSn 订单号，不能为 {@code null}
     * @param ticket 发起支付时下发的一次性票据，不能为 {@code null}
     * @throws common.exception.BaseException 票据失效 {@code ORDER_TOKEN_INVALID}、
     *         订单不存在或状态不是待付款 {@code ORDER_STATUS_INVALID}；
     *         券核销失败时转发券服务的错误码
     */
    @Transactional(rollbackFor = Exception.class)
    void handleMockPayResult(String orderSn, String ticket);

    /**
     * 发货：把已付款的订单置为已发货，写入物流信息，并通知仓库实扣库存。
     *
     * <p>只允许 {@code status} 为已付款的订单发货，靠带状态条件的 UPDATE 保证：判断与写入是同一条 SQL，
     * 并发点两次发货只有一个会成功，其余落到 {@code ORDER_STATUS_INVALID}。</p>
     *
     * <p>物流单号由本方法生成，项目未对接承运商。</p>
     *
     * <p>出库实扣走本地消息表异步执行：扣减把 {@code stock} 与 {@code stock_locked} 一起减掉同一个数，
     * 可售量（{@code stock - stock_locked}）在扣减前后完全不变，因此它不会因为缺货失败，属于可以晚一点、
     * 可以重试的清理动作。消息记录与状态变更在同一事务落库，提交后才投出去。</p>
     *
     * @param vo 发货参数，不能为 {@code null}；{@code orderSn} 与 {@code deliveryCompany} 不能为空，
     *           且 {@code deliveryCompany} 必须是 {@link order.enums.DeliveryCompanyEnum} 里的编码
     * @param operator 操作人（后台管理员用户名），取自网关注入的 {@code X-Admin} 头，写进操作史
     * @throws common.exception.BaseException 订单不存在或不是已付款状态时抛 {@code ORDER_STATUS_INVALID}
     * @throws common.exception.ValidationException 物流公司编码不在枚举内时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    void deliverOrder(DeliverVo vo, String operator);

    /**
     * 确认收货：把已发货的订单置为已完成，并写入确认状态与确认收货时间。
     *
     * <p>只允许 {@code status} 为已发货的订单确认，靠带状态条件的 UPDATE 保证：判断与写入是同一条 SQL，
     * 与自动确认任务并发时只有一个会成功，输的那个落到 {@code ORDER_STATUS_INVALID}。</p>
     *
     * <p>确认收货不计积分与成长值：{@code oms_order.integration} 与 {@code growth} 在下单时就已算好，
     * 发放属于积分业务，尚未开工。</p>
     *
     * @param user    当前登录会员，不能为 {@code null}
     * @param orderSn 订单号，不能为 {@code null}
     * @throws common.exception.BaseException 订单不存在或不属于该会员 {@code ORDER_NOT_FOUND}、
     *         状态不是已发货或已被自动确认 {@code ORDER_STATUS_INVALID}
     */
    void confirmReceive(MemberResponseVo user, String orderSn);

    /**
     * 自动确认收货：把已发货且超过 {@code auto_confirm_day} 天仍未确认的订单置为已完成。
     *
     * <p>供定时任务调用。逐单独立提交，不包在一个事务里：每一单的条件更新各自幂等，
     * 某一单失败不该回滚已经确认的其他订单。</p>
     *
     * <p>多实例安全：多个实例同时跑只会重复扫一遍，不会把订单重复流转。</p>
     *
     * @param limit 单轮最多确认多少单，必须为正数
     * @return 实际置为已完成的订单数；没有到期订单时返回 0
     */
    int autoConfirmReceive(int limit);

    /**
     * 查询支持的物流公司，供管理端发货弹窗的下拉框使用。
     *
     * <p>只读，不查库：取值就是 {@link order.enums.DeliveryCompanyEnum} 本身。</p>
     *
     * @return 物流公司选项，顺序与枚举声明一致；至少有一项
     */
    List<DeliveryCompanyVo> listDeliveryCompanies();

    /**
     * 查询订单状态选项，供管理端筛选下拉框使用。
     *
     * <p>只读，不查库：取值就是 {@link order.enums.OrderStatusEnum} 本身，状态文案只在枚举里维护一份。</p>
     *
     * @return 状态选项，顺序与枚举声明一致
     */
    List<OrderStatusOptionVo> listStatusOptions();

    /**
     * 创建秒杀订单：落订单主表与订单项，收货人取该会员的默认地址。
     *
     * <p>非幂等：同一条消息重复消费会建出重复订单。
     *
     * @param orderTo 秒杀订单消息，不能为 {@code null}；{@code orderSn}、{@code memberId}、
     *                {@code skuId}、{@code seckillPrice}、{@code num} 均不能为空
     */
    void createSeckillOrder(SeckillOrderTo orderTo);

    /**
     * 查询某个订单项能否评价，并下发写评价所需的商品快照。
     *
     * <p>供 product 提交评价前调用（{@code product/feign/OrderFeignService}）。Feign 直连不经网关，
     * 没有会员凭证可带，所以会员身份由调用方显式传参 —— 与 ware 查订单状态同一套约定。
     * 归属与状态两个判据留在订单这一侧算，调用方只拿结论，不自己拼 {@code oms_order} 的查询条件。</p>
     *
     * <p>只读，无副作用。订单项不存在与"不属于该会员"返回同一个错误码，避免变成用户枚举点。</p>
     *
     * @param orderItemId 订单项主键，不能为 {@code null}
     * @param memberId 会员 ID，不能为 {@code null}
     * @return 评价来源快照，含订单号、订单项、SKU/SPU、购买时属性与下单会员名
     * @throws common.exception.BaseException 订单项不存在或不属于该会员
     *         {@code COMMENT_ORDER_ITEM_NOT_FOUND}、订单不是已完成状态 {@code COMMENT_ORDER_NOT_FINISHED}
     */
    CommentTargetVo getCommentTarget(Long orderItemId, Long memberId);

    /**
     * 登记订单的评价时间，供 product 在评价落库成功后调用。
     *
     * <p>幂等：登记成功后 {@code comment_time} 非空，重复调用只会更新 0 行。登记失败不抛异常 ——
     * 评价的事实已经落在 product 侧的 {@code uk_order_item} 唯一键上，这里只补一个展示时间列，
     * 让调用方失败重试没有意义。</p>
     *
     * <p>登记生效时沿订单操作史留一行，操作人取会员名（秒杀单没有会员名时用会员 ID 兜底），
     * 让后台看单时知道这单被评过。</p>
     *
     * @param orderSn 订单号，不能为 {@code null}
     * @param memberId 会员 ID，不能为 {@code null}
     */
    void markCommented(String orderSn, Long memberId);
}

package order.service.impl;

import com.alipay.api.AlipayApiException;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lly835.bestpay.model.PayRequest;
import com.lly835.bestpay.model.PayResponse;
import com.lly835.bestpay.service.BestPayService;
import common.exception.BaseCodeEnum;
import common.exception.BaseException;
import common.exception.ValidationException;
import common.mq.MqConstant;
import common.mq.outbox.ReliableMqPublisher;
import common.to.OrderTo;
import common.to.mq.SeckillOrderTo;
import common.vo.PageVO;
import common.utils.R;
import common.vo.MemberResponseVo;
import lombok.extern.slf4j.Slf4j;
import order.config.AlipayTemplate;
import order.constant.OrderConstant;
import order.constant.PayConstant;
import order.dao.OrderDao;
import order.entity.OrderEntity;
import order.entity.OrderItemEntity;
import order.entity.PaymentInfoEntity;
import order.enume.OrderStatusEnum;
import order.feign.CartFeignService;
import order.feign.MemberFeignService;
import order.feign.ProductFeignService;
import order.feign.WmsFeignService;
import order.service.OrderItemService;
import order.service.OrderService;
import order.service.PaymentInfoService;
import order.to.OrderCreateTo;
import order.vo.FareItemVo;
import order.vo.FareVo;
import order.vo.MemberAddressVo;
import order.vo.OrderConfirmVo;
import order.vo.OrderItemVo;
import order.vo.OrderStatusVo;
import order.vo.OrderSubmitVo;
import order.vo.PayAsyncVo;
import order.vo.PayResultVo;
import order.vo.PayVo;
import order.vo.SkuInfoVo;
import order.vo.SkuStockVo;
import order.vo.SpuInfoVo;
import order.vo.SubmitOrderResponseVo;
import order.vo.WareFareItemResultVo;
import order.vo.WareFareQueryItemVo;
import order.vo.WareFareQueryVo;
import order.vo.WareFareVo;
import order.vo.WareSkuLockVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import common.query.PageQuery;
import order.vo.OrderPageQuery;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import static com.lly835.bestpay.enums.BestPayTypeEnum.WXPAY_NATIVE;
import static common.constant.CartConstant.CART_PREFIX;

/**
 * 订单服务实现：负责结算页组装、下单、发起支付、关单与秒杀单落库。
 *
 * <p>本类无状态、线程安全；订单号由 {@code IdWorker} 生成，会员、购物车、商品与库存数据经 Feign 获取。
 *
 * <p>远程调用失败与业务校验不通过统一抛 {@link BaseException}，由全局异常处理器转成带 code 的响应。
 */
@Slf4j
@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    @Autowired
    private MemberFeignService memberFeignService;

    @Autowired
    private CartFeignService cartFeignService;

    @Autowired
    private WmsFeignService wmsFeignService;

    @Autowired
    private ProductFeignService productFeignService;

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    // 注入具体类型而非 MqPublisher 接口：只有本地消息表那条实现保证消息记录与订单变更同事务落库
    @Autowired
    private ReliableMqPublisher reliableMqPublisher;

    @Autowired
    private PaymentInfoService paymentInfoService;

    @Autowired
    private BestPayService bestPayService;

    @Autowired
    private AlipayTemplate alipayTemplate;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;


    /** {@inheritDoc} */
    @Override
    public PageVO<OrderEntity> queryPage(PageQuery query) {
        IPage<OrderEntity> page = this.page(query.toPage());

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

    /* ═══════════════════ 当前会员与归属校验 ═══════════════════ */

    /**
     * 按订单号取订单，并确认它属于该会员。
     *
     * <p>不属于自己的订单一律按"不存在"报，不报"无权访问"：后者等于告诉调用方
     * 这个订单号存在、只是不属于你，会成为用户枚举点。
     *
     * @param user 当前登录会员
     * @param orderSn 订单号
     * @return 属于该会员的订单
     * @throws BaseException 订单不存在或不属于该会员时抛 {@code ORDER_NOT_FOUND}
     */
    private OrderEntity requireOwnOrder(MemberResponseVo user, String orderSn) {
        OrderEntity order = getOrderByOrderSn(orderSn);
        if (order == null || !user.getId().equals(order.getMemberId())) {
            throw new BaseException(BaseCodeEnum.ORDER_NOT_FOUND);
        }
        return order;
    }

    /**
     * 确认 addrId 在该会员的地址列表里。
     *
     * <p>addrId 是前端传的，而 ware 只按 id 查地址、不校验归属，必须在本地兜住。
     *
     * @param addrId 收货地址 id
     * @param addresses 该会员的全部收货地址
     * @return 命中的收货地址
     * @throws BaseException 地址不存在或不属于该会员时抛 {@code ADDRESS_NOT_FOUND}
     */
    private MemberAddressVo requireOwnAddress(Long addrId, List<MemberAddressVo> addresses) {
        if (addrId == null || addresses == null) {
            throw new BaseException(BaseCodeEnum.ADDRESS_NOT_FOUND);
        }
        return addresses.stream()
                .filter(address -> addrId.equals(address.getId()))
                .findFirst()
                .orElseThrow(() -> new BaseException(BaseCodeEnum.ADDRESS_NOT_FOUND));
    }

    /**
     * 查询该会员的全部收货地址。
     *
     * <p>按 memberId 查，天然带归属过滤。
     *
     * @param user 当前登录会员
     * @return 收货地址列表；会员服务返回 {@code null} 时给空列表
     */
    private List<MemberAddressVo> memberAddresses(MemberResponseVo user) {
        List<MemberAddressVo> addresses = memberFeignService.getAddress(user.getId());
        return addresses == null ? List.of() : addresses;
    }

    /* ═══════════════════ 结算 ═══════════════════ */

    /**
     * {@inheritDoc}
     *
     * <p>地址与购物车两个远程调用并行发起；防重令牌写入 Redis 并带过期时间，每次进入结算页都会刷新。
     */
    @Override
    public OrderConfirmVo confirmOrder(MemberResponseVo user) {
        Long memberId = user.getId();
        OrderConfirmVo confirmVo = new OrderConfirmVo();

        // 地址与购物车互不依赖，并行发。
        // ⚠️ 异步线程没有请求上下文，必须手动搬 RequestAttributes，否则 Feign 取不到 X-Member-Claims
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        CompletableFuture<List<MemberAddressVo>> addressFuture = CompletableFuture.supplyAsync(() -> {
            RequestContextHolder.setRequestAttributes(requestAttributes);
            return memberFeignService.getAddress(memberId);
        }, threadPoolExecutor);

        CompletableFuture<List<OrderItemVo>> cartFuture = CompletableFuture.supplyAsync(() -> {
            RequestContextHolder.setRequestAttributes(requestAttributes);
            R<List<OrderItemVo>> cartResult = cartFeignService.getCheckedItems();
            return cartResult.getData();
        }, threadPoolExecutor);

        List<MemberAddressVo> addresses;
        List<OrderItemVo> items;
        try {
            addresses = addressFuture.get();
            items = cartFuture.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BaseException(BaseCodeEnum.ORDER_CONFIRM_FAILED, "加载结算信息被中断，请重试");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof BaseException baseException) {
                throw baseException;
            }
            log.error("加载结算信息失败", cause);
            throw new BaseException(BaseCodeEnum.ORDER_CONFIRM_FAILED);
        }

        confirmVo.setAddresses(addresses == null ? List.of() : addresses);
        confirmVo.setItems(items == null ? List.of() : items);

        // 没有默认地址时退化为第一个；地址列表为空时保持 null
        MemberAddressVo defaultAddress = confirmVo.getAddresses().stream()
                .filter(address -> Integer.valueOf(1).equals(address.getDefaultStatus()))
                .findFirst()
                .orElseGet(() -> confirmVo.getAddresses().stream().findFirst().orElse(null));
        if (defaultAddress != null) {
            confirmVo.setDefaultAddrId(defaultAddress.getId());
        }

        // 库存
        List<Long> skuIds = confirmVo.getItems().stream()
                .map(OrderItemVo::getSkuId)
                .collect(Collectors.toList());
        // 默认给空 Map 而不是 null：前端拿到的永远是对象，不用为"车是空的"和
        // "字段没返回"写两个分支，与 cart 的 items 给 [] 同一约定
        confirmVo.setStocks(new HashMap<>());
        if (!skuIds.isEmpty()) {
            R<List<SkuStockVo>> stockResp = wmsFeignService.getSkuHasStock(skuIds);
            List<SkuStockVo> stocks = stockResp.getData();
            if (stocks != null) {
                confirmVo.setStocks(stocks.stream().collect(Collectors.toMap(
                        SkuStockVo::getSkuId, SkuStockVo::getHasStock, (first, second) -> first)));
            }
        }

        // 金额三个数都由后端算好：前端自己算一遍会和提交时的校验算成两个数
        // 没有收货地址时运费记 0：此时页面本来就不可提交，不必为一个用不上的数去计费
        FareVo fare = defaultAddress == null
                ? zeroFare(sumItemAmount(confirmVo.getItems()))
                : calcFare(defaultAddress, confirmVo.getItems());
        confirmVo.setTotalAmount(fare.getTotalAmount());
        confirmVo.setFreightAmount(fare.getFreightAmount());
        confirmVo.setPayAmount(fare.getPayAmount());
        confirmVo.setFareItems(fare.getFareItems());

        confirmVo.setIntegration(user.getIntegration());

        // 防重令牌
        String orderToken = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(
                OrderConstant.USER_ORDER_TOKEN_PREFIX + memberId,
                orderToken,
                OrderConstant.USER_ORDER_TOKEN_TIMEOUT_MINUTES,
                TimeUnit.MINUTES);
        confirmVo.setOrderToken(orderToken);

        return confirmVo;
    }

    /** {@inheritDoc} */
    @Override
    public FareVo getFare(MemberResponseVo user, Long addrId) {
        MemberAddressVo address = requireOwnAddress(addrId, memberAddresses(user));
        return calcFare(address, checkedCartItems());
    }

    /* ═══════════════════ 运费计算 ═══════════════════ */

    /**
     * 按收货地址与购物项算运费，并补齐整单的三个金额。
     *
     * <p>确认页与换地址两条路径共用本方法：同一组入参必须算出同一结果，
     * 否则提交时重算出的金额与确认页展示的不一致，会被判成价格变动。
     *
     * @param address 收货地址，必须属于当前会员，且带行政区划编码
     * @param items 已勾选的购物项，不能为 {@code null}
     * @return 三个金额与按商品拆分的运费明细
     * @throws BaseException 地址没有行政区划编码、或 ware 算不出运费时抛出
     */
    private FareVo calcFare(MemberAddressVo address, List<OrderItemVo> items) {
        if (address.getAreacode() == null || address.getAreacode().isBlank()) {
            // 区划编码是计费的入参；缺了它不是运费不准，而是整个结算页打不开，所以文案指向地址本身
            throw new BaseException(BaseCodeEnum.ORDER_FARE_FAILED, "收货地址缺少行政区划编码，请重新选择省市区");
        }

        BigDecimal totalAmount = sumItemAmount(items);
        WareFareVo wareFare = requestWareFare(address.getAreacode(), items);

        FareVo fareVo = new FareVo();
        fareVo.setTotalAmount(totalAmount);
        fareVo.setFreightAmount(wareFare.getTotalFare());
        fareVo.setPayAmount(totalAmount.add(wareFare.getTotalFare()));
        fareVo.setFareItems(toFareItems(wareFare.getItems()));
        return fareVo;
    }

    /**
     * 没有收货地址时用的金额：商品总额照常算，运费记 0。
     *
     * @param totalAmount 商品总额
     * @return 运费为 0、应付总额等于商品总额的金额对象
     */
    private FareVo zeroFare(BigDecimal totalAmount) {
        FareVo fareVo = new FareVo();
        fareVo.setTotalAmount(totalAmount);
        fareVo.setFreightAmount(BigDecimal.ZERO);
        fareVo.setPayAmount(totalAmount);
        fareVo.setFareItems(List.of());
        return fareVo;
    }

    /**
     * 汇总购物项的商品总额。
     *
     * @param items 购物项列表，不能为 {@code null}
     * @return 各项「单价 × 数量」之和
     * @throws BaseException 有购物项缺价格或数量时抛 {@code ORDER_ITEM_INCOMPLETE}
     */
    private BigDecimal sumItemAmount(List<OrderItemVo> items) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OrderItemVo item : items) {
            if (item.getPrice() == null || item.getCount() == null) {
                throw new BaseException(BaseCodeEnum.ORDER_ITEM_INCOMPLETE, "购物项价格信息不完整，请返回购物车重试");
            }
            totalAmount = totalAmount.add(item.getPrice().multiply(BigDecimal.valueOf(item.getCount())));
        }
        return totalAmount;
    }

    /**
     * 查询购物车里已勾选的购物项。
     *
     * @return 已勾选的购物项；没有勾选项时为空列表，不返回 {@code null}
     */
    private List<OrderItemVo> checkedCartItems() {
        R<List<OrderItemVo>> cartResult = cartFeignService.getCheckedItems();
        List<OrderItemVo> items = cartResult.getData();
        return items == null ? List.of() : items;
    }

    /**
     * 向 ware 请求计费。
     *
     * @param destNode 收货地区划编码，不能为 {@code null}
     * @param items 已勾选的购物项，不能为 {@code null}
     * @return ware 返回的整单运费与明细
     * @throws BaseException ware 报错时原样转发它的错误码；
     *         码为 0 却没给数据时抛 {@code ORDER_FARE_FAILED}
     */
    private WareFareVo requestWareFare(String destNode, List<OrderItemVo> items) {
        WareFareQueryVo query = new WareFareQueryVo();
        query.setDestNode(destNode);
        query.setItems(items.stream().map(item -> {
            WareFareQueryItemVo queryItem = new WareFareQueryItemVo();
            queryItem.setSkuId(item.getSkuId());
            queryItem.setNum(item.getCount());
            return queryItem;
        }).toList());

        R<WareFareVo> response = wmsFeignService.getFare(query);
        if (response.getCode() != 0) {
            // 原样转发 ware 的错误码：商品没入库和仓库没配区划编码是两回事，排查方向不同
            throw new BaseException(response.getCode(), response.getMsg());
        }
        if (response.getData() == null) {
            throw new BaseException(BaseCodeEnum.ORDER_FARE_FAILED, "仓库服务没有返回运费");
        }
        return response.getData();
    }

    /**
     * 把 ware 的运费明细转成前端契约，只保留前端要用的两个字段。
     *
     * @param wareItems ware 返回的明细，可以为 {@code null}
     * @return 只带 SKU 与运费的明细列表；{@code wareItems} 为 {@code null} 时返回空列表
     */
    private List<FareItemVo> toFareItems(List<WareFareItemResultVo> wareItems) {
        if (wareItems == null) {
            return List.of();
        }
        return wareItems.stream().map(wareItem -> {
            FareItemVo fareItem = new FareItemVo();
            fareItem.setSkuId(wareItem.getSkuId());
            fareItem.setFare(wareItem.getFare());
            return fareItem;
        }).toList();
    }

    /* ═══════════════════ 提交订单 ═══════════════════ */

    /**
     * {@inheritDoc}
     *
     * <p>整个方法在一个事务内：价格校验、订单落库、锁定库存、消息记录任一步失败都整体回滚，
     * 不会留下已写入的订单与订单项，订单创建事件也不会发出。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public SubmitOrderResponseVo submitOrder(MemberResponseVo user, OrderSubmitVo vo) {
        Long memberId = user.getId();

        // 0. 地址归属。放在最前面：后面几步都有副作用（删令牌、落库、锁库存），
        //    校验失败时必须保证什么都没动过
        MemberAddressVo address = requireOwnAddress(vo.getAddrId(), memberAddresses(user));

        // 1. 验证并消费防重令牌，用 Lua 保证比较与删除的原子性：返回 1 表示消费成功
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        Long result = redisTemplate.execute(
                new DefaultRedisScript<>(script, Long.class),
                Arrays.asList(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberId),
                vo.getOrderToken()
        );
        if (result == null || result == 0L) {
            log.info("订单令牌验证失败，memberId={}", memberId);
            throw new BaseException(BaseCodeEnum.ORDER_TOKEN_INVALID);
        }

        // 2. 创建订单与订单项
        OrderCreateTo order = createOrder(user, vo, address);

        // 3. 验证价格。必须用 compareTo：两边 scale 不同（前端传 41941，库里是 41941.0000），
        //    BigDecimal.equals 会判不等，compareTo 只比数值
        BigDecimal payAmount = order.getOrder().getPayAmount();
        if (payAmount.compareTo(vo.getPayPrice()) != 0) {
            log.info("价格验证失败：应付 {}，前端传 {}", payAmount, vo.getPayPrice());
            throw new BaseException(BaseCodeEnum.ORDER_PRICE_CHANGED);
        }

        saveOrder(order);

        // 4. 锁定库存；失败抛异常，由事务回滚已落库的订单
        WareSkuLockVo lockVo = new WareSkuLockVo();
        lockVo.setOrderSn(order.getOrder().getOrderSn());
        List<OrderItemVo> orderItemVos = order.getOrderItems().stream().map((item) -> {
            OrderItemVo orderItemVo = new OrderItemVo();
            orderItemVo.setSkuId(item.getSkuId());
            orderItemVo.setCount(item.getSkuQuantity());
            orderItemVo.setTitle(item.getSkuName());
            return orderItemVo;
        }).collect(Collectors.toList());
        lockVo.setLocks(orderItemVos);

        R<Boolean> r = wmsFeignService.orderLockStock(lockVo);
        if (r.getCode() != 0) {
            // 必须转成 BaseException：NoStockException 没有 @ExceptionHandler 接，抛出去会落到
            // Spring 默认错误页（无 code/msg），前端只能显示"请求失败"
            throw new BaseException(BaseCodeEnum.NO_STOCK_EXCEPTION, r.getMsg());
        }

        // 消息记录与订单在同一事务里落库，提交之后才真正投递：事务回滚时记录一并消失，
        // 消费者不会收到库里不存在的订单；投递失败留给本地消息表的重投任务兜底
        reliableMqPublisher.publish(MqConstant.Exchanges.ORDER, MqConstant.RoutingKeys.ORDER_CREATED, order.getOrder());

        // 清购物车回滚不了，只能等提交成功之后再清：否则事务回滚时订单没落下来、车却先空了
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                redisTemplate.delete(CART_PREFIX + memberId);
            }
        });

        SubmitOrderResponseVo responseVo = new SubmitOrderResponseVo();
        responseVo.setOrder(order.getOrder());
        return responseVo;
    }

    /* ═══════════════════ 我的订单 ═══════════════════ */

    /**
     * {@inheritDoc}
     *
     * <p>订单项按 orderSn 一次批量查出再分组，不逐单查询。
     */
    @Override
    public PageVO<OrderEntity> queryMemberOrders(MemberResponseVo user, OrderPageQuery query) {
        Long memberId = user.getId();

        LambdaQueryWrapper<OrderEntity> wrapper = new LambdaQueryWrapper<OrderEntity>()
                .eq(OrderEntity::getMemberId, memberId)
                // 只列未删除的：deleteStatus 为 0 表示未删除
                .eq(OrderEntity::getDeleteStatus, 0)
                .orderByDesc(OrderEntity::getCreateTime);

        String status = query.getStatus();
        if (status != null && !status.toString().isBlank()) {
            wrapper.eq(OrderEntity::getStatus, status);
        }

        IPage<OrderEntity> page = this.page(query.toPage(), wrapper);
        List<OrderEntity> records = page.getRecords();

        // 订单项一次查完再按 orderSn 分组：逐个订单查订单项是 N+1
        if (!records.isEmpty()) {
            List<String> orderSns = records.stream().map(OrderEntity::getOrderSn).collect(Collectors.toList());
            Map<String, List<OrderItemEntity>> itemsByOrderSn = orderItemService
                    .list(new LambdaQueryWrapper<OrderItemEntity>().in(OrderItemEntity::getOrderSn, orderSns))
                    .stream()
                    .collect(Collectors.groupingBy(OrderItemEntity::getOrderSn));
            records.forEach(order -> order.setOrderItemEntityList(
                    itemsByOrderSn.getOrDefault(order.getOrderSn(), List.of())));
        }
        fillStatusText(records);

        page.setRecords(records);
        return new PageVO<>(page.getTotal(), page.getRecords());
    }

    /** {@inheritDoc} */
    @Override
    public OrderEntity getOrderDetail(MemberResponseVo user, String orderSn) {
        OrderEntity order = requireOwnOrder(user, orderSn);
        order.setOrderItemEntityList(
                orderItemService.list(new LambdaQueryWrapper<OrderItemEntity>().eq(OrderItemEntity::getOrderSn, orderSn)));
        order.setStatusText(statusText(order.getStatus()));
        return order;
    }

    /** {@inheritDoc} */
    @Override
    public OrderEntity getOrderByOrderSn(String orderSn) {
        return this.baseMapper.selectOne(new LambdaQueryWrapper<OrderEntity>().eq(OrderEntity::getOrderSn, orderSn));
    }



    /**
     * 为订单列表填充状态文案。
     *
     * <p>文案由后端给而不是前端自己映射：{@link OrderStatusEnum} 有 7 个值，
     * 前端再维护一份就多了一个会漂移的副本。
     *
     * @param orders 待填充的订单列表
     */
    private void fillStatusText(List<OrderEntity> orders) {
        for (OrderEntity order : orders) {
            order.setStatusText(statusText(order.getStatus()));
        }
    }

    private String statusText(Integer status) {
        if (status == null) {
            return "";
        }
        for (OrderStatusEnum value : OrderStatusEnum.values()) {
            if (value.getCode().equals(status)) {
                return value.getMsg();
            }
        }
        // 不认识的码不要给前端一个 null（它要么显示空白要么得再判空）。
        // 留一条 warn：出现它就说明枚举漏了库里的取值
        log.warn("未知的订单状态码：{}", status);
        return "未知状态";
    }

    /* ═══════════════════ 支付 ═══════════════════ */

    /** {@inheritDoc} */
    @Override
    public PayResultVo payOrder(MemberResponseVo user, String orderSn, Integer payType) {
        OrderEntity order = requireOwnOrder(user, orderSn);

        // 只有待付款能发起支付：已支付/已取消的订单再拉起收银台没有意义，
        // 而支付宝那边对同一 out_trade_no 重复下单也会报错
        if (!OrderStatusEnum.CREATE_NEW.getCode().equals(order.getStatus())) {
            throw new BaseException(BaseCodeEnum.ORDER_STATUS_INVALID);
        }

        PayResultVo result = new PayResultVo();
        result.setPayType(payType);

        if (PayConstant.ALIPAY.equals(payType)) {
            try {
                result.setForm(alipayTemplate.pay(buildPayVo(orderSn)));
            } catch (AlipayApiException e) {
                log.error("发起支付宝支付失败，orderSn={}", orderSn, e);
                throw new BaseException(BaseCodeEnum.ORDER_PAY_FAILED);
            }
            return result;
        }

        if (PayConstant.WXPAY.equals(payType)) {
            PayRequest request = new PayRequest();
            request.setOrderName("谷粒商城订单 " + orderSn);
            request.setOrderId(orderSn);
            // ⚠️ 必须用订单真实的应付金额：微信侧按这个值收款，写死就与订单金额脱钩
            request.setOrderAmount(order.getPayAmount().doubleValue());
            request.setPayTypeEnum(WXPAY_NATIVE);

            PayResponse payResponse;
            try {
                payResponse = bestPayService.pay(request);
            } catch (Exception e) {
                // BestPay 在微信侧返回非 SUCCESS 时抛的是 RuntimeException（不是受检异常），
                // 不转换就会一路冒到 DispatcherServlet，前端拿到没有 code 的裸 500
                log.error("发起微信支付失败，orderSn={}", orderSn, e);
                throw new BaseException(BaseCodeEnum.ORDER_PAY_FAILED, "发起微信支付失败，请稍后重试或改用支付宝");
            }
            log.info("发起微信支付 orderSn={} response={}", orderSn, payResponse);
            result.setCodeUrl(payResponse.getCodeUrl());
            return result;
        }

        throw new ValidationException("payType", "不支持的支付方式：" + payType);
    }

    /**
     * 组装支付宝下单参数。
     *
     * <p>金额取库里的应付总额，不接受前端传参。
     *
     * @param orderSn 订单号
     * @return 支付宝下单参数
     * @throws BaseException 订单不存在时抛 {@code ORDER_NOT_FOUND}
     */
    private PayVo buildPayVo(String orderSn) {
        OrderEntity orderInfo = getOrderByOrderSn(orderSn);
        if (orderInfo == null) {
            throw new BaseException(BaseCodeEnum.ORDER_NOT_FOUND);
        }

        PayVo payVo = new PayVo();
        // 保留两位、向上取整：实付会比 payAmount 略高几分
        payVo.setTotal_amount(orderInfo.getPayAmount().setScale(2, RoundingMode.UP).toString());
        payVo.setOut_trade_no(orderInfo.getOrderSn());
        payVo.setSubject("谷粒商城订单 " + orderSn);

        // 订单项可能为空（异常数据），取下标 0 前必须判空
        List<OrderItemEntity> orderItemInfo = orderItemService.list(
                new LambdaQueryWrapper<OrderItemEntity>().eq(OrderItemEntity::getOrderSn, orderSn));
        if (!orderItemInfo.isEmpty()) {
            OrderItemEntity first = orderItemInfo.get(0);
            payVo.setSubject(first.getSkuName());
            payVo.setBody(first.getSkuAttrsVals());
        }

        return payVo;
    }

    /** {@inheritDoc} */
    @Override
    public OrderStatusVo getOrderStatus(String orderSn) {
        OrderEntity order = getOrderByOrderSn(orderSn);
        return order == null ? null : toStatusVo(order);
    }

    /** {@inheritDoc} */
    @Override
    public OrderStatusVo getMyOrderStatus(MemberResponseVo user, String orderSn) {
        return toStatusVo(requireOwnOrder(user, orderSn));
    }

    private OrderStatusVo toStatusVo(OrderEntity order) {
        OrderStatusVo vo = new OrderStatusVo();
        vo.setOrderSn(order.getOrderSn());
        vo.setStatus(order.getStatus());
        vo.setStatusText(statusText(order.getStatus()));
        return vo;
    }

    /* ═══════════════════ 取消 / 关单 ═══════════════════ */

    /**
     * {@inheritDoc}
     *
     * <p>事务边界落在这里而不是 {@code doClose}：私有方法上的 {@code @Transactional} 不生效，
     * 而关单状态与"释放库存"的消息记录必须同事务提交。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void cancelOrder(MemberResponseVo user, String orderSn) {
        OrderEntity order = requireOwnOrder(user, orderSn);
        if (!OrderStatusEnum.CREATE_NEW.getCode().equals(order.getStatus())) {
            throw new BaseException(BaseCodeEnum.ORDER_STATUS_INVALID);
        }
        doClose(order);
    }

    /**
     * {@inheritDoc}
     *
     * <p>事务边界说明同 {@link #cancelOrder}。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void closeOrder(OrderEntity orderEntity) {
        // 以库里的最新状态为准：消息重投时订单可能已被支付或取消
        OrderEntity orderInfo = this.getOne(new LambdaQueryWrapper<OrderEntity>()
                .eq(OrderEntity::getOrderSn, orderEntity.getOrderSn()));

        if (orderInfo == null) {
            // 订单不存在就跳过：监听器的 catch 是 basicReject(requeue=true)，抛异常会让消息无限重投
            log.warn("要关闭的订单不存在，跳过：{}", orderEntity.getOrderSn());
            return;
        }

        if (OrderStatusEnum.CREATE_NEW.getCode().equals(orderInfo.getStatus())) {
            doClose(orderInfo);
        }
    }

    /**
     * 关单：置为已取消并通知仓库释放库存。
     *
     * <p>超时关单（MQ）与用户主动取消共用这一段。本方法自己不承担事务边界 —— 它是私有方法，
     * 注解在此不生效；边界由两个调用者 {@code cancelOrder} 与 {@code closeOrder} 承担，
     * 保证"置为已取消"与"释放库存的消息记录"同事务提交。
     *
     * @param orderInfo 待关闭的订单
     */
    private void doClose(OrderEntity orderInfo) {
        OrderEntity orderUpdate = new OrderEntity();
        orderUpdate.setId(orderInfo.getId());
        orderUpdate.setStatus(OrderStatusEnum.CANCLED.getCode());
        this.updateById(orderUpdate);

        OrderTo orderTo = new OrderTo();
        BeanUtils.copyProperties(orderInfo, orderTo);
        // copyProperties 拷到的是关单前的对象，status 仍是待付款；ware 按库存工作单解锁、
        // 不读这个字段，但 OrderTo 是订单快照，发出去的状态必须与库里一致
        orderTo.setStatus(OrderStatusEnum.CANCLED.getCode());

        // 不吞异常：消息记录写不进去就整体回滚，宁可不关单，也不能关了单却不释放库存
        reliableMqPublisher.publish(MqConstant.Exchanges.ORDER, MqConstant.RoutingKeys.ORDER_CLOSED, orderTo);
    }

    /* ═══════════════════ 订单创建（内部） ═══════════════════ */

    private void saveOrder(OrderCreateTo orderCreateTo) {

        OrderEntity order = orderCreateTo.getOrder();
        order.setModifyTime(new Date());
        order.setCreateTime(new Date());
        this.baseMapper.insert(order);

        List<OrderItemEntity> orderItems = orderCreateTo.getOrderItems();
        orderItemService.saveBatch(orderItems);
    }


    private OrderCreateTo createOrder(MemberResponseVo user, OrderSubmitVo submitVo, MemberAddressVo address) {

        OrderCreateTo createTo = new OrderCreateTo();

        // 1. 购物车只取一次：运费与订单项都要用它，取两次会给两处结果不一致留窗口
        List<OrderItemVo> cartItems = checkedCartItems();
        if (cartItems.isEmpty()) {
            throw new BaseException(BaseCodeEnum.ORDER_CART_EMPTY);
        }

        // 2. 生成订单号；订单主表与订单项都要写它
        String orderSn = IdWorker.getTimeId();

        // 3. 按与确认页同一套逻辑重算运费，两次算出的必须是同一个数
        BigDecimal freightAmount = calcFare(address, cartItems).getFreightAmount();

        // 4. 组装订单主表与订单项
        OrderEntity orderEntity = builderOrder(user, orderSn, submitVo, address, freightAmount);
        List<OrderItemEntity> orderItemEntities = builderOrderItems(orderSn, cartItems);

        // 5. 计算价格与积分
        computePrice(orderEntity, orderItemEntities);

        createTo.setOrder(orderEntity);
        createTo.setOrderItems(orderItemEntities);

        return createTo;
    }


    private void computePrice(OrderEntity orderEntity, List<OrderItemEntity> orderItemEntities) {

        BigDecimal total = new BigDecimal("0.0");
        BigDecimal coupon = new BigDecimal("0.0");
        BigDecimal intergration = new BigDecimal("0.0");
        BigDecimal promotion = new BigDecimal("0.0");

        Integer integrationTotal = 0;
        Integer growthTotal = 0;

        for (OrderItemEntity orderItem : orderItemEntities) {
            coupon = coupon.add(orderItem.getCouponAmount());
            promotion = promotion.add(orderItem.getPromotionAmount());
            intergration = intergration.add(orderItem.getIntegrationAmount());

            total = total.add(orderItem.getRealAmount());

            integrationTotal += orderItem.getGiftIntegration();
            growthTotal += orderItem.getGiftGrowth();

        }
        orderEntity.setTotalAmount(total);
        // 应付总额 = 商品总额 + 运费
        orderEntity.setPayAmount(total.add(orderEntity.getFreightAmount()));
        orderEntity.setCouponAmount(coupon);
        orderEntity.setPromotionAmount(promotion);
        orderEntity.setIntegrationAmount(intergration);

        orderEntity.setIntegration(integrationTotal);
        orderEntity.setGrowth(growthTotal);

        // 0 表示未删除
        orderEntity.setDeleteStatus(0);

    }


    /**
     * 组装订单主表：收货信息、运费与初始状态。
     *
     * @param user 当前登录会员
     * @param orderSn 已生成的订单号
     * @param submitVo 提交入参，提供收货地址 id 与备注
     * @param address 收货地址，地址归属已在 {@code submitOrder} 入口校验
     * @param freightAmount 已重算的运费
     * @return 未落库的订单实体
     */
    private OrderEntity builderOrder(MemberResponseVo user, String orderSn, OrderSubmitVo submitVo,
                                     MemberAddressVo address, BigDecimal freightAmount) {

        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setMemberId(user.getId());
        orderEntity.setOrderSn(orderSn);
        orderEntity.setMemberUsername(user.getUsername());
        orderEntity.setNote(submitVo.getRemarks());
        orderEntity.setFreightAmount(freightAmount);

        orderEntity.setReceiverName(address.getName());
        orderEntity.setReceiverPhone(address.getPhone());
        orderEntity.setReceiverPostCode(address.getPostCode());
        orderEntity.setReceiverProvince(address.getProvince());
        orderEntity.setReceiverCity(address.getCity());
        orderEntity.setReceiverRegion(address.getRegion());
        orderEntity.setReceiverDetailAddress(address.getDetailAddress());

        orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        orderEntity.setAutoConfirmDay(7);
        orderEntity.setConfirmStatus(0);
        return orderEntity;
    }

    /**
     * 构建订单包含的全部订单项。
     *
     * @param orderSn 订单号，写入每个订单项
     * @param cartItems 购物车中已勾选的购物项，不能为 {@code null}
     * @return 订单项列表；购物车没有已勾选商品时返回空列表
     */
    private List<OrderItemEntity> builderOrderItems(String orderSn, List<OrderItemVo> cartItems) {
        return cartItems.stream().map(cartItem -> {
            OrderItemEntity orderItemEntity = builderOrderItem(cartItem);
            orderItemEntity.setOrderSn(orderSn);
            return orderItemEntity;
        }).collect(Collectors.toList());
    }


    /**
     * 构建单个订单项，补齐 SPU/SKU 信息与优惠、积分字段。
     *
     * @param items 购物车中已勾选的购物项，skuId、价格、数量都不能为空
     * @return 订单项实体，{@code orderSn} 由调用方写入
     */
    private OrderItemEntity builderOrderItem(OrderItemVo items) {

        if (items.getSkuId() == null || items.getPrice() == null || items.getCount() == null) {
            throw new BaseException(BaseCodeEnum.ORDER_ITEM_INCOMPLETE, "购物项信息不完整（skuId/价格/数量），请返回购物车重试");
        }

        OrderItemEntity orderItemEntity = new OrderItemEntity();

        // 1. 商品的 SPU 信息
        Long skuId = items.getSkuId();
        R<SpuInfoVo> spuInfo = productFeignService.getSpuInfoBySkuId(skuId);
        SpuInfoVo spuInfoData = spuInfo.getData();
        if (spuInfoData == null) {
            // 商品服务查不到时，下面几行会把订单项写成 spuId/spuName 全空的脏数据，且不报错
            throw new BaseException(BaseCodeEnum.PRODUCT_NOT_FOUND, "商品 " + skuId + " 的 SPU 信息缺失，无法下单");
        }
        orderItemEntity.setSpuId(spuInfoData.getId());
        orderItemEntity.setSpuName(spuInfoData.getSpuName());
        orderItemEntity.setSpuBrand(spuInfoData.getBrandName());
        orderItemEntity.setCategoryId(spuInfoData.getCatalogId());

        // 2. 商品的 SKU 信息
        orderItemEntity.setSkuId(skuId);
        orderItemEntity.setSkuName(items.getTitle());
        orderItemEntity.setSkuPic(items.getImage());
        orderItemEntity.setSkuPrice(items.getPrice());
        orderItemEntity.setSkuQuantity(items.getCount());

        // 销售属性可能为 null（该 sku 没有销售属性），collectionToDelimitedString 不接 null
        List<String> skuAttrValues = items.getSkuAttrValues();
        orderItemEntity.setSkuAttrsVals(skuAttrValues == null
                ? ""
                : StringUtils.collectionToDelimitedString(skuAttrValues, ";"));

        // 3. 商品的优惠信息

        // 4. 商品的积分信息
        orderItemEntity.setGiftGrowth(items.getPrice().multiply(new BigDecimal(items.getCount())).intValue());
        orderItemEntity.setGiftIntegration(items.getPrice().multiply(new BigDecimal(items.getCount())).intValue());

        // 5. 订单项的价格信息
        orderItemEntity.setPromotionAmount(BigDecimal.ZERO);
        orderItemEntity.setCouponAmount(BigDecimal.ZERO);
        orderItemEntity.setIntegrationAmount(BigDecimal.ZERO);

        // 实付金额 = 原价（单价 × 数量）减去各类优惠
        BigDecimal origin = orderItemEntity.getSkuPrice().multiply(new BigDecimal(orderItemEntity.getSkuQuantity().toString()));
        BigDecimal subtract = origin.subtract(orderItemEntity.getCouponAmount())
                .subtract(orderItemEntity.getPromotionAmount())
                .subtract(orderItemEntity.getIntegrationAmount());
        orderItemEntity.setRealAmount(subtract);

        return orderItemEntity;
    }


    /**
     * {@inheritDoc}
     *
     * <p>流水与订单状态在同一事务内写入，两者必须一起生效。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public String handlePayResult(PayAsyncVo asyncVo) {

        PaymentInfoEntity paymentInfo = new PaymentInfoEntity();
        paymentInfo.setOrderSn(asyncVo.getOut_trade_no());
        paymentInfo.setAlipayTradeNo(asyncVo.getTrade_no());
        paymentInfo.setTotalAmount(new BigDecimal(asyncVo.getBuyer_pay_amount()));
        paymentInfo.setSubject(asyncVo.getBody());
        paymentInfo.setPaymentStatus(asyncVo.getTrade_status());
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setCallbackTime(asyncVo.getNotify_time());
        this.paymentInfoService.save(paymentInfo);

        // 只有这两个交易状态代表付款成功，其余状态不动订单
        String tradeStatus = asyncVo.getTrade_status();

        if (tradeStatus.equals("TRADE_SUCCESS") || tradeStatus.equals("TRADE_FINISHED")) {
            String orderSn = asyncVo.getOut_trade_no();
            this.updateOrderStatus(orderSn,OrderStatusEnum.PAYED.getCode(), PayConstant.ALIPAY);
        }

        return "success";
    }


    /**
     * 更新订单的支付状态与支付方式。
     *
     * @param orderSn 订单号
     * @param code 目标状态码，取自 {@link OrderStatusEnum}
     * @param payType 支付方式，取自 {@code PayConstant}
     */
    private void updateOrderStatus(String orderSn, Integer code,Integer payType) {

        this.baseMapper.updateOrderStatus(orderSn,code,payType);
    }

    /** {@inheritDoc} */
    @Override
    public String asyncNotify(String notifyData) {

        // 1. 签名校验
        PayResponse payResponse = bestPayService.asyncNotify(notifyData);
        log.info("payResponse={}",payResponse);

        // 2. 查订单
        OrderEntity orderEntity = this.getOrderByOrderSn(payResponse.getOrderId());

        if (orderEntity == null) {
            // TODO: 订单查不到时发出告警（钉钉、短信），当前只中断这条通知
            throw new RuntimeException("通过订单编号查询出来的结果是null");
        }

        // 幂等：已支付或已取消的订单不再重复推进
        Integer status = orderEntity.getStatus();
        if (status.equals(OrderStatusEnum.PAYED.getCode()) || status.equals(OrderStatusEnum.CANCLED.getCode())) {
            throw new RuntimeException("该订单已失效,orderNo=" + payResponse.getOrderId());
        }

        // TODO: 微信异步通知的金额未与订单应付金额比对，需补校验并在不一致时告警

        // 3. 修改订单支付状态
        String orderSn = orderEntity.getOrderSn();
        this.updateOrderStatus(orderSn,OrderStatusEnum.PAYED.getCode(),PayConstant.WXPAY);

        // 4. 应答 SUCCESS，微信收到后不再重复通知
        return "<xml>\n" +
                "  <return_code><![CDATA[SUCCESS]]></return_code>\n" +
                "  <return_msg><![CDATA[OK]]></return_msg>\n" +
                "</xml>";
    }


    /** {@inheritDoc} */
    @Override
    public void createSeckillOrder(SeckillOrderTo orderTo) {

        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderSn(orderTo.getOrderSn());
        orderEntity.setMemberId(orderTo.getMemberId());
        orderEntity.setCreateTime(new Date());
        BigDecimal totalPrice = orderTo.getSeckillPrice().multiply(BigDecimal.valueOf(orderTo.getNum()));
        orderEntity.setPayAmount(totalPrice);
        orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());

        List<MemberAddressVo> address = memberFeignService.getAddress(orderTo.getMemberId());
        if (address == null || address.isEmpty()) {
            throw new BaseException(BaseCodeEnum.ADDRESS_NOT_FOUND);
        }
        // 与 confirmOrder 同一套选法：优先默认地址，没有就退化为第一个
        MemberAddressVo addressVo = address.stream()
                .filter(add -> Integer.valueOf(1).equals(add.getDefaultStatus()))
                .findFirst()
                .orElse(address.get(0));
        orderEntity.setReceiverName(addressVo.getName());
        orderEntity.setReceiverPhone(addressVo.getPhone());
        orderEntity.setReceiverProvince(addressVo.getProvince());
        orderEntity.setReceiverCity(addressVo.getCity());
        orderEntity.setReceiverRegion(addressVo.getRegion());
        orderEntity.setReceiverDetailAddress(addressVo.getDetailAddress());

        this.save(orderEntity);

        OrderItemEntity orderItem = new OrderItemEntity();
        orderItem.setOrderSn(orderTo.getOrderSn());
        orderItem.setRealAmount(totalPrice);

        orderItem.setSkuQuantity(orderTo.getNum());

        R<SpuInfoVo> spuInfo = productFeignService.getSpuInfoBySkuId(orderTo.getSkuId());
        SpuInfoVo spuInfoData = spuInfo.getData();
        orderItem.setSpuId(spuInfoData.getId());
        orderItem.setSpuName(spuInfoData.getSpuName());
        orderItem.setSpuBrand(spuInfoData.getBrandName());
        orderItem.setCategoryId(spuInfoData.getCatalogId());

        R<SkuInfoVo> skuInfo = productFeignService.getSkuInfoBySkuId(orderTo.getSkuId());
        SkuInfoVo skuInfoVo = skuInfo.getData();
        orderItem.setSkuName(skuInfoVo.getSkuName());
        orderItem.setSkuPic(skuInfoVo.getSkuDefaultImg());
        orderItem.setSkuPrice(skuInfoVo.getPrice());
        orderItem.setPromotionAmount(new BigDecimal(0));
        orderItem.setCouponAmount(new BigDecimal(0));
        orderItem.setIntegrationAmount(new BigDecimal(0));
        orderItem.setGiftGrowth(orderTo.getSeckillPrice().intValueExact());
        orderItem.setGiftIntegration(orderTo.getSeckillPrice().intValueExact());
        orderItemService.save(orderItem);
    }


}

package order.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alipay.api.AlipayApiException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lly835.bestpay.model.PayRequest;
import com.lly835.bestpay.model.PayResponse;
import com.lly835.bestpay.service.BestPayService;
import common.exception.BaseCodeEnum;
import common.exception.BaseException;
import common.exception.ValidationException;
import common.mq.MqConstant;
import common.mq.MqPublisher;
import common.to.OrderTo;
import common.to.mq.SeckillOrderTo;
import common.utils.PageUtils;
import common.utils.Query;
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
import order.vo.WareSkuLockVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

import static com.lly835.bestpay.enums.BestPayTypeEnum.WXPAY_NATIVE;
import static common.constant.CartConstant.CART_PREFIX;

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

    @Autowired
    private MqPublisher mqPublisher;

    @Autowired
    private PaymentInfoService paymentInfoService;

    @Autowired
    private BestPayService bestPayService;

    @Autowired
    private AlipayTemplate alipayTemplate;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

    /* ═══════════════════ 当前会员与归属校验 ═══════════════════ */

    /**
     * 按订单号取订单，并确认它属于该会员。不属于自己的订单一律按"不存在"报，不报"无权访问" ——
     * 后者等于告诉调用方"这个订单号存在，只是不属于你"，那是个用户枚举点。
     */
    private OrderEntity requireOwnOrder(MemberResponseVo user, String orderSn) {
        OrderEntity order = getOrderByOrderSn(orderSn);
        if (order == null || !user.getId().equals(order.getMemberId())) {
            throw new BaseException(BaseCodeEnum.ORDER_NOT_FOUND);
        }
        return order;
    }

    /** 确认 addrId 在该会员的地址列表里：addrId 是前端传的，而 ware 只按 id 查地址、不校验归属 */
    private MemberAddressVo requireOwnAddress(Long addrId, List<MemberAddressVo> addresses) {
        if (addrId == null || addresses == null) {
            throw new BaseException(BaseCodeEnum.ADDRESS_NOT_FOUND);
        }
        return addresses.stream()
                .filter(address -> addrId.equals(address.getId()))
                .findFirst()
                .orElseThrow(() -> new BaseException(BaseCodeEnum.ADDRESS_NOT_FOUND));
    }

    /** 该会员的全部收货地址。按 memberId 查，天然带归属过滤 */
    private List<MemberAddressVo> memberAddresses(MemberResponseVo user) {
        List<MemberAddressVo> addresses = memberFeignService.getAddress(user.getId());
        return addresses == null ? List.of() : addresses;
    }

    /* ═══════════════════ 结算 ═══════════════════ */

    @Override
    public OrderConfirmVo confirmOrder(MemberResponseVo user) {
        Long memberId = user.getId();
        OrderConfirmVo confirmVo = new OrderConfirmVo();

        // 三个远程调用互不依赖，并行发。
        // ⚠️ 每个异步任务里都要把 RequestAttributes 传进去：cart 的接口要求登录，
        //    而 X-Member-Claims 是 FeignConfig 从"当前请求"里取的，异步线程默认没有请求上下文
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        CompletableFuture<List<MemberAddressVo>> addressFuture = CompletableFuture.supplyAsync(() -> {
            RequestContextHolder.setRequestAttributes(requestAttributes);
            return memberFeignService.getAddress(memberId);
        }, threadPoolExecutor);

        CompletableFuture<List<OrderItemVo>> cartFuture = CompletableFuture.supplyAsync(() -> {
            RequestContextHolder.setRequestAttributes(requestAttributes);
            R cartResult = cartFeignService.getCheckedItems();
            return cartResult.getData("data", new TypeReference<List<OrderItemVo>>() {});
        }, threadPoolExecutor);

        List<MemberAddressVo> addresses;
        List<OrderItemVo> items;
        try {
            addresses = addressFuture.get();
            items = cartFuture.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BaseException("加载结算信息被中断，请重试");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof BaseException baseException) {
                throw baseException;
            }
            log.error("加载结算信息失败", cause);
            throw new BaseException("加载结算信息失败，请稍后重试");
        }

        confirmVo.setAddresses(addresses == null ? List.of() : addresses);
        confirmVo.setItems(items == null ? List.of() : items);

        // 默认地址：优先 defaultStatus == 1，没有默认就取第一个；一个地址都没有时为 null
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
        // 默认给空 Map 而不是留 null：前端拿到的永远是对象，不用为"车是空的"和
        // "字段没返回"写两个分支（和 cart 那边 items 给 [] 不给 null 是同一个理由）
        confirmVo.setStocks(new HashMap<>());
        if (!skuIds.isEmpty()) {
            R stockResp = wmsFeignService.getSkuHasStock(skuIds);
            List<SkuStockVo> stocks = stockResp.getData("data", new TypeReference<List<SkuStockVo>>() {});
            if (stocks != null) {
                confirmVo.setStocks(stocks.stream().collect(Collectors.toMap(
                        SkuStockVo::getSkuId, SkuStockVo::getHasStock, (first, second) -> first)));
            }
        }

        // 金额：三个数都由后端算好给前端。前端只负责显示和原样回传 payAmount，
        // 自己算一遍就会和提交时的校验算成两个数（老页面正是这么坏的）
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OrderItemVo item : confirmVo.getItems()) {
            if (item.getPrice() == null || item.getCount() == null) {
                throw new BaseException("购物项价格信息不完整，请返回购物车重试");
            }
            totalAmount = totalAmount.add(item.getPrice().multiply(BigDecimal.valueOf(item.getCount())));
        }
        confirmVo.setTotalAmount(totalAmount);

        BigDecimal freightAmount = BigDecimal.ZERO;
        if (defaultAddress != null) {
            // 地址是从会员自己的列表里取出来的，归属已经成立，不用再查一遍
            freightAmount = fetchFare(defaultAddress.getId()).getFare();
        }
        confirmVo.setFreightAmount(freightAmount);
        confirmVo.setPayAmount(totalAmount.add(freightAmount));

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

    @Override
    public FareVo getFare(MemberResponseVo user, Long addrId) {
        requireOwnAddress(addrId, memberAddresses(user));
        return fetchFare(addrId);
    }

    /** 真正去 ware 取运费。归属校验由调用方负责 */
    private FareVo fetchFare(Long addrId) {
        R fareResp = wmsFeignService.getFare(addrId);
        FareVo fare = fareResp.getData("data", new TypeReference<FareVo>() {});
        if (fare == null || fare.getFare() == null) {
            // ware 在地址查不到时返回 data=null，原来这里直接 getFare() 会 NPE 成 500
            throw new BaseException("运费计算失败，请检查收货地址");
        }
        return fare;
    }

    /* ═══════════════════ 提交订单 ═══════════════════ */

    /**
     * 提交订单。
     *
     * <p>失败不再靠 {@code SubmitOrderResponseVo.code} 的 1/2/3 —— 那个命名空间和 {@code R.code}
     * （0=成功）是两套，控制器套上 R 之后前端要先判 body.code 再判 data.code，极容易漏判成功。
     * 现在统一抛 {@code BaseException}，走 {@code R.error(code, msg)}。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public SubmitOrderResponseVo submitOrder(MemberResponseVo user, OrderSubmitVo vo) {
        Long memberId = user.getId();

        // 0、地址归属。放在最前面：其它步骤都有副作用（删令牌、落库、锁库存），
        //    校验失败时要保证什么都没动过
        requireOwnAddress(vo.getAddrId(), memberAddresses(user));

        // 1、验证令牌是否合法【令牌的对比和删除必须保证原子性】
        //    0 令牌失败 - 1 删除成功
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

        // 2、创建订单、订单项等信息
        OrderCreateTo order = createOrder(user, vo);

        // 3、验证价格。用 compareTo 而不是相减比 double：两边 scale 不同（前端传的是
        //    41941，库里算出来是 41941.0000），BigDecimal.equals 会判不等，compareTo 只比数值
        BigDecimal payAmount = order.getOrder().getPayAmount();
        if (payAmount.compareTo(vo.getPayPrice()) != 0) {
            log.info("价格验证失败：应付 {}，前端传 {}", payAmount, vo.getPayPrice());
            throw new BaseException(BaseCodeEnum.ORDER_PRICE_CHANGED);
        }

        saveOrder(order);

        // 4、锁定库存。只要有异常，回滚订单数据
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

        R r = wmsFeignService.orderLockStock(lockVo);
        if (r.getCode() != 0) {
            // 用 BaseException 而不是 NoStockException：后者没有任何 @ExceptionHandler 接，
            // 抛出去会落到 Spring 默认错误页（没有 code/msg），前端只能显示"请求失败"。
            // ware 那边是本地 try/catch 处理 NoStockException 的，跨服务传过来就只剩 msg 了
            throw new BaseException(BaseCodeEnum.NO_STOCK_EXCEPTION, (String) r.get("msg"));
        }

        // TODO 阶段 4：下面两件事都发生在事务提交**之前**，是错的 ——
        //   消息已经发出去了但事务可能回滚（消费者会收到一个数据库里不存在的订单），
        //   购物车也已经删了但订单可能没落库。要改成注册 TransactionSynchronization.afterCommit。
        // TODO 订单创建成功，发送消息给MQ
        mqPublisher.publish(MqConstant.Exchanges.ORDER_EVENT, MqConstant.RoutingKeys.ORDER_CREATE, order.getOrder());
        // 删除购物车里的数据
        redisTemplate.delete(CART_PREFIX + memberId);

        SubmitOrderResponseVo responseVo = new SubmitOrderResponseVo();
        responseVo.setOrder(order.getOrder());
        return responseVo;
    }

    /* ═══════════════════ 我的订单 ═══════════════════ */

    @Override
    public PageUtils queryMemberOrders(MemberResponseVo user, Map<String, Object> params) {
        Long memberId = user.getId();

        QueryWrapper<OrderEntity> wrapper = new QueryWrapper<OrderEntity>()
                .eq("member_id", memberId)
                // 已删除的不展示。老的 queryPageWithItem 没有这个过滤，用户删过的订单还会再列出来
                .eq("delete_status", 0)
                .orderByDesc("create_time");

        Object status = params.get("status");
        if (status != null && !status.toString().isBlank()) {
            wrapper.eq("status", status);
        }

        IPage<OrderEntity> page = this.page(buildPage(params), wrapper);
        List<OrderEntity> records = page.getRecords();

        // 订单项一次查完再按 orderSn 分组。原来的写法是遍历订单、每个订单查一次订单项（N+1）
        if (!records.isEmpty()) {
            List<String> orderSns = records.stream().map(OrderEntity::getOrderSn).collect(Collectors.toList());
            Map<String, List<OrderItemEntity>> itemsByOrderSn = orderItemService
                    .list(new QueryWrapper<OrderItemEntity>().in("order_sn", orderSns))
                    .stream()
                    .collect(Collectors.groupingBy(OrderItemEntity::getOrderSn));
            records.forEach(order -> order.setOrderItemEntityList(
                    itemsByOrderSn.getOrDefault(order.getOrderSn(), List.of())));
        }
        fillStatusText(records);

        page.setRecords(records);
        return new PageUtils(page);
    }

    @Override
    public OrderEntity getOrderDetail(MemberResponseVo user, String orderSn) {
        OrderEntity order = requireOwnOrder(user, orderSn);
        order.setOrderItemEntityList(
                orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", orderSn)));
        order.setStatusText(statusText(order.getStatus()));
        return order;
    }

    @Override
    public OrderEntity getOrderByOrderSn(String orderSn) {
        return this.baseMapper.selectOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderSn));
    }

    /**
     * 分页参数。
     *
     * <p>用 {@code pageNum / pageSize} 而不是 renren 那套 {@code page / limit}：
     * 前台已经有一套分页协议（检索页就是 pageNum/pageSize），同一批对接前端的接口
     * 不该出现两种页码参数名。</p>
     */
    private Page<OrderEntity> buildPage(Map<String, Object> params) {
        long pageNum = parseNumber(params.get("pageNum"), 1, "pageNum");
        long pageSize = parseNumber(params.get("pageSize"), OrderConstant.DEFAULT_PAGE_SIZE, "pageSize");
        if (pageSize > OrderConstant.MAX_PAGE_SIZE) {
            pageSize = OrderConstant.MAX_PAGE_SIZE;
        }
        return new Page<>(pageNum, pageSize);
    }

    private long parseNumber(Object raw, long defaultValue, String name) {
        if (raw == null || raw.toString().isBlank()) {
            return defaultValue;
        }
        long value;
        try {
            value = Long.parseLong(raw.toString().trim());
        } catch (NumberFormatException e) {
            throw new ValidationException(name, name + " 参数类型不正确");
        }
        if (value < 1) {
            throw new ValidationException(name, name + " 必须大于 0");
        }
        return value;
    }

    /**
     * 填状态文案。
     *
     * <p>由后端给而不是前端自己映射：{@link OrderStatusEnum} 有 7 个值，
     * 前端再维护一份就多了一个会漂移的副本。</p>
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
                throw new BaseException("发起支付失败，请稍后重试");
            }
            return result;
        }

        if (PayConstant.WXPAY.equals(payType)) {
            PayRequest request = new PayRequest();
            request.setOrderName("谷粒商城订单 " + orderSn);
            request.setOrderId(orderSn);
            // ⚠️ 用订单真实的应付金额。原来这里写死 0.01 —— 微信侧永远只收一分钱，
            //    和订单金额完全脱钩
            request.setOrderAmount(order.getPayAmount().doubleValue());
            request.setPayTypeEnum(WXPAY_NATIVE);

            PayResponse payResponse;
            try {
                payResponse = bestPayService.pay(request);
            } catch (Exception e) {
                // BestPay 在微信侧返回非 SUCCESS 时抛的是 RuntimeException，不是受检异常 ——
                // 实测（商户密钥还是配置里的 demo 值）微信回的是"签名错误"，然后这个异常
                // 一路冒到 DispatcherServlet，前端拿到的是一个**没有 code 的裸 500**。
                log.error("发起微信支付失败，orderSn={}", orderSn, e);
                throw new BaseException("发起微信支付失败，请稍后重试或改用支付宝");
            }
            log.info("发起微信支付 orderSn={} response={}", orderSn, payResponse);
            result.setCodeUrl(payResponse.getCodeUrl());
            return result;
        }

        throw new ValidationException("payType", "不支持的支付方式：" + payType);
    }

    /** 组装支付宝下单参数。金额取库里的应付总额，不接受前端传 */
    private PayVo buildPayVo(String orderSn) {
        OrderEntity orderInfo = getOrderByOrderSn(orderSn);
        if (orderInfo == null) {
            throw new BaseException(BaseCodeEnum.ORDER_NOT_FOUND);
        }

        PayVo payVo = new PayVo();
        // 保留两位、向上取整 —— 沿用原来的写法（注意：这会让实付比 payAmount 略高几分）
        payVo.setTotal_amount(orderInfo.getPayAmount().setScale(2, RoundingMode.UP).toString());
        payVo.setOut_trade_no(orderInfo.getOrderSn());
        payVo.setSubject("谷粒商城订单 " + orderSn);

        // 订单项可能为空（异常数据），原来的 get(0) 会直接 IndexOutOfBounds
        List<OrderItemEntity> orderItemInfo = orderItemService.list(
                new QueryWrapper<OrderItemEntity>().eq("order_sn", orderSn));
        if (!orderItemInfo.isEmpty()) {
            OrderItemEntity first = orderItemInfo.get(0);
            payVo.setSubject(first.getSkuName());
            payVo.setBody(first.getSkuAttrsVals());
        }

        return payVo;
    }

    @Override
    public OrderStatusVo getOrderStatus(String orderSn) {
        // ⚠️ 订单不存在时返回 null，**不能抛异常**：
        //    ware 的契约是"没有这个订单 → 必须解锁库存"（WareSkuServiceImpl#unLockStock
        //    判断 orderInfo == null 就解锁）。它先看 R.code，非 0 会抛异常让消息重投，
        //    那样库存反而永远解锁不掉。
        OrderEntity order = getOrderByOrderSn(orderSn);
        return order == null ? null : toStatusVo(order);
    }

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

    @Override
    public void cancelOrder(MemberResponseVo user, String orderSn) {
        OrderEntity order = requireOwnOrder(user, orderSn);
        if (!OrderStatusEnum.CREATE_NEW.getCode().equals(order.getStatus())) {
            throw new BaseException(BaseCodeEnum.ORDER_STATUS_INVALID);
        }
        doClose(order);
    }

    @Override
    public void closeOrder(OrderEntity orderEntity) {
        // 关闭订单之前先查询一下数据库，判断此订单状态是否已支付
        OrderEntity orderInfo = this.getOne(new QueryWrapper<OrderEntity>()
                .eq("order_sn", orderEntity.getOrderSn()));

        if (orderInfo == null) {
            // 订单不存在就跳过。原来这里直接 orderInfo.getStatus() 会 NPE，
            // 而监听器的 catch 是 basicReject(requeue=true)，NPE 会让消息无限重投
            log.warn("要关闭的订单不存在，跳过：{}", orderEntity.getOrderSn());
            return;
        }

        if (OrderStatusEnum.CREATE_NEW.getCode().equals(orderInfo.getStatus())) {
            // 待付款状态进行关单
            doClose(orderInfo);
        }
    }

    /**
     * 关单：置为已取消并通知仓库释放库存。超时关单（MQ）和用户主动取消共用这一段。
     */
    private void doClose(OrderEntity orderInfo) {
        OrderEntity orderUpdate = new OrderEntity();
        orderUpdate.setId(orderInfo.getId());
        orderUpdate.setStatus(OrderStatusEnum.CANCLED.getCode());
        this.updateById(orderUpdate);

        OrderTo orderTo = new OrderTo();
        BeanUtils.copyProperties(orderInfo, orderTo);
        // copyProperties 拷的是"关单前"的对象，status 还是待付款。
        // 说明一下为什么还是要覆盖：ware 收到这条消息后走的 unlockStock(OrderTo) 并不读 status
        // （它按库存工作单直接解锁），所以这一行不是功能必需。但 OrderTo 是订单的快照，
        // 发一个和数据库事实相反的状态出去，迟早会坑到下一个读它的人。
        orderTo.setStatus(OrderStatusEnum.CANCLED.getCode());

        try {
            // TODO 阶段 4：先改库再发消息，发失败就丢了解锁库存的机会（这里只打日志）。
            //   要做的是本地消息表 + 定时重投，或者让 ware 那边容忍漏消息
            mqPublisher.publish(MqConstant.Exchanges.ORDER_EVENT, MqConstant.RoutingKeys.ORDER_RELEASE_OTHER, orderTo);
        } catch (Exception e) {
            log.error("发送库存释放消息失败，orderSn={}", orderInfo.getOrderSn(), e);
        }
    }

    /* ═══════════════════ 订单创建（内部） ═══════════════════ */

    private void saveOrder(OrderCreateTo orderCreateTo) {

        //获取订单信息
        OrderEntity order = orderCreateTo.getOrder();
        order.setModifyTime(new Date());
        order.setCreateTime(new Date());
        //保存订单
        this.baseMapper.insert(order);

        //获取订单项信息
        List<OrderItemEntity> orderItems = orderCreateTo.getOrderItems();
        //批量保存订单项数据
        orderItemService.saveBatch(orderItems);
    }


    private OrderCreateTo createOrder(MemberResponseVo user, OrderSubmitVo submitVo) {

        OrderCreateTo createTo = new OrderCreateTo();

        //1、生成订单号
        String orderSn = IdWorker.getTimeId();
        OrderEntity orderEntity = builderOrder(user, orderSn, submitVo);

        //2、获取到所有的订单项
        List<OrderItemEntity> orderItemEntities = builderOrderItems(orderSn);
        if (orderItemEntities.isEmpty()) {
            // 提交期间购物车可能被清空或全部取消勾选（另一个标签页、或者上一次提交已经把车删了）。
            // 不挡的话会建出一张"只有运费、没有商品"的空单，而且金额校验还能通过
            throw new BaseException("购物车里没有已勾选的商品，无法提交订单");
        }

        //3、验价(计算价格、积分等信息)
        computePrice(orderEntity,orderItemEntities);

        createTo.setOrder(orderEntity);
        createTo.setOrderItems(orderItemEntities);

        return createTo;
    }


    private void computePrice(OrderEntity orderEntity, List<OrderItemEntity> orderItemEntities) {

        //总价
        BigDecimal total = new BigDecimal("0.0");
        //优惠价
        BigDecimal coupon = new BigDecimal("0.0");
        BigDecimal intergration = new BigDecimal("0.0");
        BigDecimal promotion = new BigDecimal("0.0");

        //积分、成长值
        Integer integrationTotal = 0;
        Integer growthTotal = 0;

        //订单总额，叠加每一个订单项的总额信息
        for (OrderItemEntity orderItem : orderItemEntities) {
            //优惠价格信息
            coupon = coupon.add(orderItem.getCouponAmount());
            promotion = promotion.add(orderItem.getPromotionAmount());
            intergration = intergration.add(orderItem.getIntegrationAmount());

            //总价
            total = total.add(orderItem.getRealAmount());

            //积分信息和成长值信息
            integrationTotal += orderItem.getGiftIntegration();
            growthTotal += orderItem.getGiftGrowth();

        }
        //1、订单价格相关的
        orderEntity.setTotalAmount(total);
        //设置应付总额(总额+运费)
        orderEntity.setPayAmount(total.add(orderEntity.getFreightAmount()));
        orderEntity.setCouponAmount(coupon);
        orderEntity.setPromotionAmount(promotion);
        orderEntity.setIntegrationAmount(intergration);

        //设置积分成长值信息
        orderEntity.setIntegration(integrationTotal);
        orderEntity.setGrowth(growthTotal);

        //设置删除状态(0-未删除，1-已删除)
        orderEntity.setDeleteStatus(0);

    }


    /** 组装订单主表。提交用的 {@link OrderSubmitVo} 和会员身份都从参数传进来 */
    private OrderEntity builderOrder(MemberResponseVo user, String orderSn, OrderSubmitVo submitVo) {

        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setMemberId(user.getId());
        orderEntity.setOrderSn(orderSn);
        orderEntity.setMemberUsername(user.getUsername());
        orderEntity.setNote(submitVo.getRemarks());

        //远程获取收货地址和运费信息
        // ⚠️ 地址归属已经在 submitOrder 入口校验过了，这里拿到的 fareResp 一定对应自己的地址
        FareVo fareResp = fetchFare(submitVo.getAddrId());

        //获取到运费信息
        orderEntity.setFreightAmount(fareResp.getFare());

        //获取到收货地址信息
        MemberAddressVo address = fareResp.getAddress();
        //设置收货人信息
        orderEntity.setReceiverName(address.getName());
        orderEntity.setReceiverPhone(address.getPhone());
        orderEntity.setReceiverPostCode(address.getPostCode());
        orderEntity.setReceiverProvince(address.getProvince());
        orderEntity.setReceiverCity(address.getCity());
        orderEntity.setReceiverRegion(address.getRegion());
        orderEntity.setReceiverDetailAddress(address.getDetailAddress());

        //设置订单相关的状态信息
        orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        orderEntity.setAutoConfirmDay(7);
        orderEntity.setConfirmStatus(0);
        return orderEntity;
    }

    /**
     * 构建所有订单项数据
     * @return
     */
    public List<OrderItemEntity> builderOrderItems(String orderSn) {

        List<OrderItemEntity> orderItemEntityList = new ArrayList<>();

        //最后确定每个购物项的价格
        R cartResult = cartFeignService.getCheckedItems();
        List<OrderItemVo> currentCartItems =
                cartResult.getData("data", new TypeReference<List<OrderItemVo>>() {});
        if (currentCartItems != null && !currentCartItems.isEmpty()) {
            orderItemEntityList = currentCartItems.stream().map((items) -> {
                //构建订单项数据
                OrderItemEntity orderItemEntity = builderOrderItem(items);
                orderItemEntity.setOrderSn(orderSn);

                return orderItemEntity;
            }).collect(Collectors.toList());
        }

        return orderItemEntityList;
    }


    /**
     * 构建某一个订单项的数据
     * @param items
     * @return
     */
    private OrderItemEntity builderOrderItem(OrderItemVo items) {

        if (items.getSkuId() == null || items.getPrice() == null || items.getCount() == null) {
            throw new BaseException("购物项信息不完整（skuId/价格/数量），请返回购物车重试");
        }

        OrderItemEntity orderItemEntity = new OrderItemEntity();

        //1、商品的spu信息
        Long skuId = items.getSkuId();
        //获取spu的信息
        R spuInfo = productFeignService.getSpuInfoBySkuId(skuId);
        SpuInfoVo spuInfoData = spuInfo.getData("data", new TypeReference<SpuInfoVo>() {
        });
        if (spuInfoData == null) {
            // 商品服务查不到就用的话，下面几行 setXxx(null) 会把订单项写成一条
            // spuId/spuName 全空的脏数据，而且不报任何错
            throw new BaseException("商品 " + skuId + " 的 SPU 信息缺失，无法下单");
        }
        orderItemEntity.setSpuId(spuInfoData.getId());
        orderItemEntity.setSpuName(spuInfoData.getSpuName());
        orderItemEntity.setSpuBrand(spuInfoData.getBrandName());
        orderItemEntity.setCategoryId(spuInfoData.getCatalogId());

        //2、商品的sku信息
        orderItemEntity.setSkuId(skuId);
        orderItemEntity.setSkuName(items.getTitle());
        orderItemEntity.setSkuPic(items.getImage());
        orderItemEntity.setSkuPrice(items.getPrice());
        orderItemEntity.setSkuQuantity(items.getCount());

        //使用StringUtils.collectionToDelimitedString将list集合转换为String
        // 销售属性可能为 null（该 sku 没有销售属性），collectionToDelimitedString 不接 null
        List<String> skuAttrValues = items.getSkuAttrValues();
        orderItemEntity.setSkuAttrsVals(skuAttrValues == null
                ? ""
                : StringUtils.collectionToDelimitedString(skuAttrValues, ";"));

        //3、商品的优惠信息

        //4、商品的积分信息
        orderItemEntity.setGiftGrowth(items.getPrice().multiply(new BigDecimal(items.getCount())).intValue());
        orderItemEntity.setGiftIntegration(items.getPrice().multiply(new BigDecimal(items.getCount())).intValue());

        //5、订单项的价格信息
        orderItemEntity.setPromotionAmount(BigDecimal.ZERO);
        orderItemEntity.setCouponAmount(BigDecimal.ZERO);
        orderItemEntity.setIntegrationAmount(BigDecimal.ZERO);

        //当前订单项的实际金额.总额 - 各种优惠价格
        //原来的价格
        BigDecimal origin = orderItemEntity.getSkuPrice().multiply(new BigDecimal(orderItemEntity.getSkuQuantity().toString()));
        //原价减去优惠价得到最终的价格
        BigDecimal subtract = origin.subtract(orderItemEntity.getCouponAmount())
                .subtract(orderItemEntity.getPromotionAmount())
                .subtract(orderItemEntity.getIntegrationAmount());
        orderItemEntity.setRealAmount(subtract);

        return orderItemEntity;
    }


    /**
     * 处理支付宝的支付结果
     * @param asyncVo
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public String handlePayResult(PayAsyncVo asyncVo) {

        //保存交易流水信息
        PaymentInfoEntity paymentInfo = new PaymentInfoEntity();
        paymentInfo.setOrderSn(asyncVo.getOut_trade_no());
        paymentInfo.setAlipayTradeNo(asyncVo.getTrade_no());
        paymentInfo.setTotalAmount(new BigDecimal(asyncVo.getBuyer_pay_amount()));
        paymentInfo.setSubject(asyncVo.getBody());
        paymentInfo.setPaymentStatus(asyncVo.getTrade_status());
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setCallbackTime(asyncVo.getNotify_time());
        //添加到数据库中
        this.paymentInfoService.save(paymentInfo);

        //修改订单状态
        //获取当前状态
        String tradeStatus = asyncVo.getTrade_status();

        if (tradeStatus.equals("TRADE_SUCCESS") || tradeStatus.equals("TRADE_FINISHED")) {
            //支付成功状态
            String orderSn = asyncVo.getOut_trade_no(); //获取订单号
            this.updateOrderStatus(orderSn,OrderStatusEnum.PAYED.getCode(), PayConstant.ALIPAY);
        }

        return "success";
    }


    /**
     * 修改订单状态
     * @param orderSn
     * @param code
     */
    private void updateOrderStatus(String orderSn, Integer code,Integer payType) {

        this.baseMapper.updateOrderStatus(orderSn,code,payType);
    }

    /**
     * 微信异步通知结果
     * @param notifyData
     * @return
     */
    @Override
    public String asyncNotify(String notifyData) {

        //签名效验
        PayResponse payResponse = bestPayService.asyncNotify(notifyData);
        log.info("payResponse={}",payResponse);

        //2.金额效验（从数据库查订单）
        OrderEntity orderEntity = this.getOrderByOrderSn(payResponse.getOrderId());

        //如果查询出来的数据是null的话
        //比较严重(正常情况下是不会发生的)发出告警：钉钉、短信
        if (orderEntity == null) {
            //TODO 发出告警，钉钉，短信
            throw new RuntimeException("通过订单编号查询出来的结果是null");
        }

        //判断订单状态状态是否为已支付或者是已取消,如果不是订单状态不是已支付状态
        Integer status = orderEntity.getStatus();
        if (status.equals(OrderStatusEnum.PAYED.getCode()) || status.equals(OrderStatusEnum.CANCLED.getCode())) {
            throw new RuntimeException("该订单已失效,orderNo=" + payResponse.getOrderId());
        }

        /*//判断金额是否一致,Double类型比较大小，精度问题不好控制
        if (orderEntity.getPayAmount().compareTo(BigDecimal.valueOf(payResponse.getOrderAmount())) != 0) {
            //TODO 告警
            throw new RuntimeException("异步通知中的金额和数据库里的不一致,orderNo=" + payResponse.getOrderId());
        }*/

        //3.修改订单支付状态
        //支付成功状态
        String orderSn = orderEntity.getOrderSn();
        this.updateOrderStatus(orderSn,OrderStatusEnum.PAYED.getCode(),PayConstant.WXPAY);

        //4.告诉微信不要再重复通知了
        return "<xml>\n" +
                "  <return_code><![CDATA[SUCCESS]]></return_code>\n" +
                "  <return_msg><![CDATA[OK]]></return_msg>\n" +
                "</xml>";
    }


    /**
     * 创建秒杀单
     * @param orderTo
     */
    @Override
    public void createSeckillOrder(SeckillOrderTo orderTo) {

        //TODO 保存订单信息
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderSn(orderTo.getOrderSn());
        orderEntity.setMemberId(orderTo.getMemberId());
        orderEntity.setCreateTime(new Date());
        BigDecimal totalPrice = orderTo.getSeckillPrice().multiply(BigDecimal.valueOf(orderTo.getNum()));
        orderEntity.setPayAmount(totalPrice);
        orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());

        List<MemberAddressVo> address = memberFeignService.getAddress(orderTo.getMemberId());
        MemberAddressVo addressVo = address.stream().filter(add -> add.getDefaultStatus() != null).collect(Collectors.toList()).get(0);
        orderEntity.setReceiverName(addressVo.getName());
        orderEntity.setReceiverPhone(addressVo.getPhone());
        orderEntity.setReceiverProvince(addressVo.getProvince());
        orderEntity.setReceiverCity(addressVo.getCity());
        orderEntity.setReceiverRegion(addressVo.getRegion());
        orderEntity.setReceiverDetailAddress(addressVo.getDetailAddress());

        //保存订单
        this.save(orderEntity);

        //保存订单项信息
        OrderItemEntity orderItem = new OrderItemEntity();
        orderItem.setOrderSn(orderTo.getOrderSn());
        orderItem.setRealAmount(totalPrice);

        orderItem.setSkuQuantity(orderTo.getNum());

        //保存商品的spu信息
        R spuInfo = productFeignService.getSpuInfoBySkuId(orderTo.getSkuId());
        SpuInfoVo spuInfoData = spuInfo.getData("data", new TypeReference<SpuInfoVo>() {
        });
        orderItem.setSpuId(spuInfoData.getId());
        orderItem.setSpuName(spuInfoData.getSpuName());
        orderItem.setSpuBrand(spuInfoData.getBrandName());
        orderItem.setCategoryId(spuInfoData.getCatalogId());

        R skuInfo = productFeignService.getSkuInfoBySkuId(orderTo.getSkuId());
        SkuInfoVo skuInfoVo = skuInfo.getData("skuInfo", new TypeReference<SkuInfoVo>() {
        });
        orderItem.setSkuName(skuInfoVo.getSkuName());
        orderItem.setSkuPic(skuInfoVo.getSkuDefaultImg());
        orderItem.setSkuPic(skuInfoVo.getSkuDefaultImg());
        orderItem.setSkuPrice(skuInfoVo.getPrice());
        orderItem.setSkuPrice(skuInfoVo.getPrice());
        orderItem.setSkuAttrsVals(orderItem.getSkuAttrsVals());
        orderItem.setPromotionAmount(new BigDecimal(0));
        orderItem.setCouponAmount(new BigDecimal(0));
        orderItem.setIntegrationAmount(new BigDecimal(0));
        orderItem.setGiftGrowth(orderTo.getSeckillPrice().intValueExact());
        orderItem.setGiftIntegration(orderTo.getSeckillPrice().intValueExact());
        //保存订单项数据
        orderItemService.save(orderItem);
    }


}

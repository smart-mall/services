package order.service.impl;

import com.alibaba.fastjson.JSON;
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
import common.to.CommentBriefVo;
import common.to.CommentTargetVo;
import common.to.CouponCartItemVo;
import common.to.CouponUsableVo;
import common.to.MemberGrowthVo;
import common.to.OrderTo;
import common.to.SkuBoundsVo;
import common.to.SkuPromotionVo;
import common.to.mq.OrderDeliveredTo;
import common.to.mq.SeckillOrderTo;
import common.vo.PageVO;
import common.utils.GiftCalculator;
import common.utils.R;
import common.vo.MemberResponseVo;
import lombok.extern.slf4j.Slf4j;
import order.config.AlipayTemplate;
import order.constant.IntegrationConstant;
import order.constant.OrderConstant;
import order.constant.PayConstant;
import order.dao.OrderDao;
import order.dao.OrderReturnApplyDao;
import order.entity.OrderEntity;
import order.entity.OrderItemEntity;
import order.entity.OrderOperateHistoryEntity;
import order.entity.OrderReturnApplyEntity;
import order.entity.OrderReturnReasonEntity;
import order.entity.PaymentInfoEntity;
import order.entity.RefundInfoEntity;
import order.enums.AfterSaleStatusEnum;
import order.enums.AfterSaleTypeEnum;
import order.enums.DeliveryCompanyEnum;
import order.enums.OrderStatusEnum;
import order.feign.CartFeignService;
import order.feign.CouponFeignService;
import order.feign.MemberFeignService;
import order.feign.MockPayFeignService;
import order.feign.ProductFeignService;
import order.feign.WmsFeignService;
import order.service.OrderItemService;
import order.service.OrderOperateHistoryService;
import order.service.OrderReturnApplyService;
import order.service.OrderReturnReasonService;
import order.service.OrderService;
import order.service.PaymentInfoService;
import order.service.RefundInfoService;
import order.to.OrderCreateTo;
import order.vo.AfterSaleAuditVo;
import order.vo.AfterSalePageQuery;
import order.vo.CouponDiscountItemVo;
import order.vo.DeliverVo;
import order.vo.DeliveryCompanyVo;
import order.vo.FareItemVo;
import order.vo.FareVo;
import order.vo.IntegrationDiscountItemVo;
import order.vo.MemberAddressVo;
import order.vo.MemberAfterSaleVo;
import order.vo.MockRefundRequestVo;
import order.vo.MockRefundResultVo;
import order.vo.OrderConfirmVo;
import order.vo.OrderItemVo;
import order.vo.OrderStatusOptionVo;
import order.vo.OrderStatusVo;
import order.vo.OrderSubmitVo;
import order.vo.PayAsyncVo;
import order.vo.PayResultVo;
import order.vo.PayVo;
import order.vo.PromotionDiscountItemVo;
import order.vo.RefundApplyVo;
import order.vo.ReturnApplyVo;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import order.vo.OrderPageQuery;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import static com.lly835.bestpay.enums.BestPayTypeEnum.WXPAY_NATIVE;
import static common.constant.CartConstant.CART_PREFIX;

/**
 * 订单服务实现：负责结算页组装、下单、发起支付、发货、关单、收货确认、订单删除与秒杀单落库。
 *
 * <p>本类无状态、线程安全；订单号由 {@code IdWorker} 生成，会员、购物车、商品与库存数据经 Feign 获取。
 *
 * <p>远程调用失败与业务校验不通过统一抛 {@link BaseException}，由全局异常处理器转成带 code 的响应。
 */
@Slf4j
@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    /** 订单的"已删除"标记，对应 {@code oms_order.delete_status}。 */
    private static final Integer DELETED_STATUS = 1;

    /** 操作史说明文字的长度上限，与 {@code oms_order_operate_history.note} 列一致。 */
    private static final int NOTE_MAX_LENGTH = 500;

    @Autowired
    private MemberFeignService memberFeignService;

    @Autowired
    private CartFeignService cartFeignService;

    @Autowired
    private WmsFeignService wmsFeignService;

    @Autowired
    private ProductFeignService productFeignService;

    @Autowired
    private CouponFeignService couponFeignService;

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
    private OrderReturnApplyService orderReturnApplyService;

    /** 售后申请表的 Mapper：四条状态流转都带状态条件，直接用它的自定义 SQL。 */
    @Autowired
    private OrderReturnApplyDao orderReturnApplyDao;

    @Autowired
    private OrderReturnReasonService orderReturnReasonService;

    @Autowired
    private RefundInfoService refundInfoService;

    @Autowired
    private OrderOperateHistoryService orderOperateHistoryService;

    @Autowired
    private BestPayService bestPayService;

    @Autowired
    private AlipayTemplate alipayTemplate;

    /** 模拟支付平台收银台的对外地址，取 {@code gl.pay.mock.cashier-url}。 */
    @Value("${gl.pay.mock.cashier-url}")
    private String mockCashierUrl;

    /** 模拟支付平台的退款接口地址，取 {@code gl.pay.mock.refund-url}。 */
    @Autowired
    private MockPayFeignService mockPayFeignService;

    /** 商户身份凭证，取 {@code gl.pay.mock.merchant-sign}，与 {@code mock-pay/nginx.conf} 里的判断同值。 */
    @Value("${gl.pay.mock.merchant-sign}")
    private String mockMerchantSign;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    /** 微信要求的成功应答，原样回给微信；微信收到后即停止重发通知。 */
    private static final String WX_SUCCESS_RESPONSE = "<xml>\n" +
            "  <return_code><![CDATA[SUCCESS]]></return_code>\n" +
            "  <return_msg><![CDATA[OK]]></return_msg>\n" +
            "</xml>";


    /**
     * {@inheritDoc}
     *
     * <p>关键字跨订单号、收货人、会员名三列，三个 or 用一个 and 包住：不包的话 or 会把后面的
     * 状态与时间条件一起放开，查出不匹配的订单。</p>
     */
    @Override
    public PageVO<OrderEntity> queryPage(OrderPageQuery query) {
        LambdaQueryWrapper<OrderEntity> wrapper = new LambdaQueryWrapper<>();

        String key = query.getKey();
        if (key != null && !key.isBlank()) {
            wrapper.and(inner -> inner.like(OrderEntity::getOrderSn, key)
                    .or().like(OrderEntity::getReceiverName, key)
                    .or().like(OrderEntity::getMemberUsername, key));
        }

        String status = query.getStatus();
        if (status != null && !status.isBlank()) {
            wrapper.eq(OrderEntity::getStatus, status);
        }

        String startTime = query.getStartTime();
        if (startTime != null && !startTime.isBlank()) {
            wrapper.ge(OrderEntity::getCreateTime, startTime);
        }

        String endTime = query.getEndTime();
        if (endTime != null && !endTime.isBlank()) {
            wrapper.le(OrderEntity::getCreateTime, endTime);
        }

        // 后台不按删除状态过滤：会员删掉的订单仍要能查到，否则无法审单、也无法硬删
        String deleteStatus = query.getDeleteStatus();
        if (deleteStatus != null && !deleteStatus.isBlank()) {
            wrapper.eq(OrderEntity::getDeleteStatus, deleteStatus);
        }

        // 同一秒下的单按主键兜底，否则翻页可能重复或漏单
        wrapper.orderByDesc(OrderEntity::getCreateTime).orderByDesc(OrderEntity::getId);

        IPage<OrderEntity> page = this.page(query.toPage(), wrapper);
        fillStatusText(page.getRecords());

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
     *
     * <p>券的两段查询是串行的且必须在算完金额之后：可用券列表要用购物项内容去匹配，
     * 选中券的抵扣额又要并进应付金额。
     *
     * <p>积分排在券之后：抵扣上限按"商品总额减券抵扣"算，顺序反了会多抵一笔。
     * 余额取自会员服务而不是凭证里那份登录时的快照。
     */
    @Override
    public OrderConfirmVo confirmOrder(MemberResponseVo user, Long couponHistoryId, Integer useIntegration) {
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

        // 金额四个数都由后端算好：前端自己算一遍会和提交时的校验算成两个数
        // 没有收货地址时运费记 0：此时页面本来就不可提交，不必为一个用不上的数去计费
        FareVo fare = defaultAddress == null
                ? zeroFare(sumItemAmount(confirmVo.getItems()))
                : calcFare(defaultAddress, confirmVo.getItems(), isFreeFreight(memberId));
        confirmVo.setTotalAmount(fare.getTotalAmount());
        confirmVo.setFreightAmount(fare.getFreightAmount());
        confirmVo.setFareItems(fare.getFareItems());
        confirmVo.setPromotionAmount(fare.getPromotionAmount());
        confirmVo.setPromotionItems(fare.getPromotionItems());

        // 券：先列出本车可用的，再按选中那张把优惠并进应付金额与逐项明细。
        // 抵扣额并进 payAmount 而不是让前端再减一次：前端只显示和原样回传这一个数
        // 逐项促销减免一起带过去：券服务按促销后金额判门槛，这是它唯一的基数来源
        List<CouponCartItemVo> couponCartItems = toCouponCartItems(confirmVo.getItems(), fare.getPromotionItems());
        confirmVo.setAvailableCoupons(listUsableCoupons(memberId, couponCartItems));
        CouponUsableVo selected = applyCoupon(fare, memberId, couponCartItems, couponHistoryId);
        confirmVo.setCouponHistoryId(selected == null ? null : selected.getCouponHistoryId());
        confirmVo.setCouponAmount(fare.getCouponAmount());
        confirmVo.setCouponItems(fare.getCouponItems());

        // 积分：排在券之后算，抵扣上限按"商品总额减券抵扣"的一半取。
        // 余额查不到就按 0 降级：它只决定结算页上这一栏显不显示，不并进任何金额
        int integrationBalance = optionalIntegrationBalance(memberId);
        confirmVo.setIntegration(integrationBalance);
        confirmVo.setIntegrationLimit(
                applyIntegration(fare, confirmVo.getItems(), integrationBalance, useIntegration));
        confirmVo.setIntegrationAmount(fare.getIntegrationAmount());
        confirmVo.setIntegrationItems(fare.getIntegrationItems());
        // 应付金额必须在券与积分都并进去之后再取，否则前端拿到的价与提交时的校验对不上
        confirmVo.setPayAmount(fare.getPayAmount());

        // 本单可得的赠送量：与下单入账用的是同一份算法，这里只用于展示
        GiftCalculator.Amount gift = totalGift(confirmVo.getItems());
        confirmVo.setGiftIntegration(gift.integration());
        confirmVo.setGiftGrowth(gift.growth());

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
    public FareVo getFare(MemberResponseVo user, Long addrId, Long couponHistoryId, Integer useIntegration) {
        MemberAddressVo address = requireOwnAddress(addrId, memberAddresses(user));
        List<OrderItemVo> items = checkedCartItems();
        FareVo fare = calcFare(address, items, isFreeFreight(user.getId()));
        // 换地址只该覆盖运费。这里不跟着减一次券与积分的话，前端换完地址拿到的应付金额会退回原价，
        // 提交时被 price 校验判成"商品价格已变动"
        applyCoupon(fare, user.getId(), toCouponCartItems(items, fare.getPromotionItems()), couponHistoryId);
        // 与确认页同一套：余额查不到按 0 降级，只影响这一栏的展示
        fare.setIntegrationLimit(
                applyIntegration(fare, items, optionalIntegrationBalance(user.getId()), useIntegration));

        // 与确认页同一份：换地址不该让"本单可得"那一行消失
        GiftCalculator.Amount gift = totalGift(items);
        fare.setGiftIntegration(gift.integration());
        fare.setGiftGrowth(gift.growth());
        return fare;
    }

    /* ═══════════════════ 运费计算 ═══════════════════ */

    /**
     * 按收货地址与购物项算运费，并补齐商品总额与应付总额。
     *
     * <p>确认页与换地址两条路径共用本方法：同一组入参必须算出同一结果，
     * 否则提交时重算出的金额与确认页展示的不一致，会被判成价格变动。
     *
     * <p>购物项为空时不去问 ware：ware 对空清单返回的是参数校验错误，那种响应解不成运费对象。
     * 空车本来就允许出现，此时只算商品总额（为 0），运费与优惠都记 0。
     *
     * <p>促销减免在这里算好并并进应付金额；券与积分两个字段留空，由 {@code applyCoupon} 与
     * {@code applyIntegration} 按选中的券与积分数补。
     *
     * @param address 收货地址，必须属于当前会员，且带行政区划编码
     * @param items 已勾选的购物项，不能为 {@code null}；为空时不再计费
     * @param freeFreight 当前会员的等级是否带免邮特权；为 {@code true} 时运费与逐项明细都记 0
     * @return 商品总额、运费、应付总额、运费明细，以及为 0 的优惠金额与为空的优惠明细
     * @throws BaseException 地址没有行政区划编码、或 ware 算不出运费时抛出
     */
    private FareVo calcFare(MemberAddressVo address, List<OrderItemVo> items, boolean freeFreight) {
        if (address.getAreacode() == null || address.getAreacode().isBlank()) {
            // 区划编码是计费的入参；缺了它不是运费不准，而是整个结算页打不开，所以文案指向地址本身
            throw new BaseException(BaseCodeEnum.ORDER_FARE_FAILED, "收货地址缺少行政区划编码，请重新选择省市区");
        }

        BigDecimal totalAmount = sumItemAmount(items);
        // 购物车为空时直接给零运费，不问 ware：ware 对空清单返回的是参数校验错误，
        // 那种响应里的 data 是 {字段名: 提示} 的形状，解不成运费对象
        if (items.isEmpty()) {
            return zeroFare(totalAmount);
        }

        WareFareVo wareFare = requestWareFare(address.getAreacode(), items);

        // 会员等级带免邮特权时整单运费与逐项明细一起归零：只改总额会让前端
        // 按明细逐行显示的运费加起来不等于整单运费
        BigDecimal freightAmount = freeFreight ? BigDecimal.ZERO : wareFare.getTotalFare();

        // 促销排在券与积分之前：它减的是商品金额本身，后两者的基数都要先扣掉它
        List<PromotionDiscountItemVo> promotionItems = promote(items);
        BigDecimal promotionAmount = sumDiscount(promotionItems);

        FareVo fareVo = new FareVo();
        fareVo.setTotalAmount(totalAmount);
        fareVo.setFreightAmount(freightAmount);
        fareVo.setPromotionAmount(promotionAmount);
        fareVo.setPromotionItems(promotionItems);
        fareVo.setPayAmount(totalAmount.add(freightAmount).subtract(promotionAmount));
        fareVo.setFareItems(toFareItems(wareFare.getItems(), freeFreight));
        fareVo.setCouponAmount(BigDecimal.ZERO);
        fareVo.setCouponItems(List.of());
        return fareVo;
    }

    /**
     * 没有收货地址时用的金额：商品总额照常算，运费记 0。
     *
     * @param totalAmount 商品总额
     * @return 运费与优惠都为 0、应付总额等于商品总额的金额对象
     */
    private FareVo zeroFare(BigDecimal totalAmount) {
        FareVo fareVo = new FareVo();
        fareVo.setTotalAmount(totalAmount);
        fareVo.setFreightAmount(BigDecimal.ZERO);
        fareVo.setPayAmount(totalAmount);
        fareVo.setFareItems(List.of());
        fareVo.setPromotionAmount(BigDecimal.ZERO);
        fareVo.setPromotionItems(List.of());
        fareVo.setCouponAmount(BigDecimal.ZERO);
        fareVo.setCouponItems(List.of());
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
     * @param freeFreight 当前会员是否免邮；为 {@code true} 时每项运费记 0，
     *                    与整单运费归零保持一致
     * @return 只带 SKU 与运费的明细列表；{@code wareItems} 为 {@code null} 时返回空列表
     */
    private List<FareItemVo> toFareItems(List<WareFareItemResultVo> wareItems, boolean freeFreight) {
        if (wareItems == null) {
            return List.of();
        }
        return wareItems.stream().map(wareItem -> {
            FareItemVo fareItem = new FareItemVo();
            fareItem.setSkuId(wareItem.getSkuId());
            fareItem.setFare(freeFreight ? BigDecimal.ZERO : wareItem.getFare());
            return fareItem;
        }).toList();
    }

    /**
     * 查当前会员的等级是否带免邮特权。
     *
     * <p>失败开放：查不到时按不免邮算并记 warn。判据与促销同源 —— 确认页与提交走的是同一个方法，
     * 两次都按不免邮算，会员看到的就是实际要付的；真出现两次结果不同（会员服务恢复），
     * 提交会落到「价格已变动」，会员刷新结算页重试即可，不会按错价成交。
     *
     * @param memberId 会员 id，不能为 {@code null}
     * @return {@code true} 表示当前等级免邮
     */
    private boolean isFreeFreight(Long memberId) {
        R<MemberGrowthVo> response;
        try {
            response = memberFeignService.growthLevel(memberId);
        } catch (Exception e) {
            log.warn("查询会员等级权益失败，本单按不免邮处理: {}", e.getMessage());
            return false;
        }

        if (response == null || response.getCode() != 0 || response.getData() == null) {
            log.warn("查询会员等级权益失败，本单按不免邮处理: {}", response == null ? "响应为空" : response.getMsg());
            return false;
        }
        return response.getData().isFreeFreight();
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
        OrderEntity newOrder = order.getOrder();
        WareSkuLockVo lockVo = new WareSkuLockVo();
        lockVo.setOrderSn(newOrder.getOrderSn());
        // 收货信息按订单上的快照带过去，ware 据此建出的工作单才是自包含的，发货不必回查订单
        lockVo.setOrderId(newOrder.getId());
        lockVo.setConsignee(newOrder.getReceiverName());
        lockVo.setConsigneeTel(newOrder.getReceiverPhone());
        lockVo.setDeliveryAddress(joinDeliveryAddress(newOrder));
        lockVo.setOrderComment(newOrder.getNote());
        lockVo.setPaymentWay(toPaymentWay(vo.getPayType()));
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
        reliableMqPublisher.publish(MqConstant.Exchanges.ORDER, MqConstant.RoutingKeys.ORDER_CREATED, newOrder);

        // 清购物车回滚不了，只能等提交成功之后再清：否则事务回滚时订单没落下来、车却先空了
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                redisTemplate.delete(CART_PREFIX + memberId);
            }
        });

        // 5. 占用优惠券，放在最后一步：它写在券服务自己的事务里，本事务回滚不会跟着回滚，
        //    越靠后剩下的可失败步骤越少
        lockCoupon(memberId, newOrder, vo.getCouponHistoryId());

        // 6. 抵扣积分。同样写在会员服务自己的事务里，靠回滚补偿退回；
        //    排在占券之后，任一失败的补偿都只涉及已经成功的那些步骤
        deductIntegration(memberId, newOrder.getOrderSn(), newOrder.getUseIntegration());

        SubmitOrderResponseVo responseVo = new SubmitOrderResponseVo();
        responseVo.setOrder(order.getOrder());
        return responseVo;
    }

    /* ═══════════════════ 商品促销 ═══════════════════ */

    /**
     * 算出每个购物项命中的商品促销减免额。
     *
     * <p>促销由商品上的阶梯价与满减两段组成，规则存在 coupon 库、由券服务下发，
     * 但命中哪一档要看本行的金额与件数，所以判定在这里做。
     *
     * <p>两段的先后是阶梯价先改单价、满减再按折后行金额判：会员实际要付的是折后金额，
     * 满减门槛用原价判会让"打完折已不满门槛"的订单也被减掉。
     *
     * <p>取不到规则时按没有促销算并记 warn。判据是促销不是会员选中的凭证：确认页与提交
     * 走的是同一个方法，两次都不命中，会员看到的就是实际要付的，不构成"页面显示减了、
     * 提交却按原价"；而券是会员明确选中的一张，静默不抵扣等于骗他，所以那边失败关闭。
     *
     * @param cartItems 已勾选的购物项，不能为 {@code null}
     * @return 命中促销的购物项及其减免额；一项都没命中时为空列表，不返回 {@code null}
     */
    private List<PromotionDiscountItemVo> promote(List<OrderItemVo> cartItems) {
        if (cartItems.isEmpty()) {
            return List.of();
        }

        List<Long> skuIds = cartItems.stream()
                .map(OrderItemVo::getSkuId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (skuIds.isEmpty()) {
            return List.of();
        }
        Map<Long, SkuPromotionVo> rules = loadSkuPromotions(skuIds);

        List<PromotionDiscountItemVo> hits = new ArrayList<>();
        for (OrderItemVo item : cartItems) {
            if (item.getPrice() == null || item.getCount() == null) {
                continue;
            }
            BigDecimal discount = promotionAmount(item, rules.get(item.getSkuId()));
            if (discount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            PromotionDiscountItemVo hit = new PromotionDiscountItemVo();
            hit.setSkuId(item.getSkuId());
            hit.setDiscountAmount(discount);
            hits.add(hit);
        }
        return hits;
    }

    /**
     * 按 skuId 批量取促销规则。
     *
     * <p>失败开放，判据见 {@link #promote}。
     *
     * @param skuIds SKU 标识集合，不能为空
     * @return skuId 到规则的映射；没配过促销的 SKU 不在映射里，不返回 {@code null}
     */
    private Map<Long, SkuPromotionVo> loadSkuPromotions(List<Long> skuIds) {
        R<List<SkuPromotionVo>> response;
        try {
            response = couponFeignService.skuRules(skuIds);
        } catch (Exception e) {
            log.warn("取商品促销规则失败，本单按无促销处理: {}", e.getMessage());
            return Map.of();
        }

        if (response == null || response.getCode() != 0 || response.getData() == null) {
            log.warn("取商品促销规则失败，本单按无促销处理: {}", response == null ? "响应为空" : response.getMsg());
            return Map.of();
        }

        return response.getData().stream()
                .filter(rule -> rule.getSkuId() != null)
                .collect(Collectors.toMap(SkuPromotionVo::getSkuId, rule -> rule, (first, second) -> second));
    }

    /**
     * 算一个购物项的促销减免：阶梯价的单价差额加上满减。
     *
     * @param item 购物项，单价与数量必须已判过非空
     * @param rule 该 SKU 的规则；为 {@code null} 表示没配过促销
     * @return 减免额，最小为 0，且不超过该项的原价总额
     */
    private BigDecimal promotionAmount(OrderItemVo item, SkuPromotionVo rule) {
        if (rule == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal originAmount = itemAmount(item);
        BigDecimal ladderOff = ladderDiscount(item, rule);
        BigDecimal total = ladderOff.add(fullReduction(originAmount.subtract(ladderOff), rule));
        return total.min(originAmount);
    }

    /**
     * 算阶梯价的减免：按件数命中档位后，用折后单价与原价的差额乘件数。
     *
     * <p>折后单价优先取配置里的 {@code price}：那是后台直接填的折后价，比按折扣反算更具体；
     * 没填折后价才用 {@code discount} 乘原价，结果向下取到分。
     *
     * @param item 购物项，单价与数量必须已判过非空
     * @param rule 该 SKU 的规则，不能为 {@code null}
     * @return 减免额；没配阶梯价、件数不到档、或折后价不低于原价时返回 0
     */
    private BigDecimal ladderDiscount(OrderItemVo item, SkuPromotionVo rule) {
        if (rule.getFullCount() == null || rule.getFullCount() <= 0 || item.getCount() < rule.getFullCount()) {
            return BigDecimal.ZERO;
        }

        BigDecimal unitPrice = item.getPrice();
        BigDecimal ladderUnitPrice = rule.getLadderPrice() != null
                ? rule.getLadderPrice()
                : ladderUnitPriceByDiscount(unitPrice, rule.getDiscount());
        if (ladderUnitPrice == null || ladderUnitPrice.compareTo(unitPrice) >= 0) {
            return BigDecimal.ZERO;
        }
        return unitPrice.subtract(ladderUnitPrice).multiply(BigDecimal.valueOf(item.getCount()));
    }

    /**
     * 按折扣率算折后单价。
     *
     * @param unitPrice 原价单价，不能为 {@code null}
     * @param discount 折扣率，如 {@code 0.75}；为 {@code null} 表示没配
     * @return 折后单价，向下取到分；没配折扣时返回 {@code null}
     */
    private BigDecimal ladderUnitPriceByDiscount(BigDecimal unitPrice, BigDecimal discount) {
        return discount == null ? null : unitPrice.multiply(discount).setScale(2, RoundingMode.DOWN);
    }

    /**
     * 算满减的减免额：行金额达到门槛就减，但不会减得比行金额还多。
     *
     * <p>门槛与减免额都是后台配的数，两者可能配成"满 400 减 400"遇上一行刚好 400，
     * 所以要与行金额取小，不能让订单项出现负金额。
     *
     * @param amountAfterLadder 阶梯价之后的行金额，不能为 {@code null}
     * @param rule 该 SKU 的规则，不能为 {@code null}
     * @return 减免额；没配满减或没到门槛时返回 0
     */
    private BigDecimal fullReduction(BigDecimal amountAfterLadder, SkuPromotionVo rule) {
        if (rule.getFullPrice() == null || rule.getReducePrice() == null) {
            return BigDecimal.ZERO;
        }
        if (amountAfterLadder.compareTo(rule.getFullPrice()) < 0) {
            return BigDecimal.ZERO;
        }
        return rule.getReducePrice().min(amountAfterLadder);
    }

    /**
     * 汇总各购物项的促销减免额。
     *
     * @param promotionItems 命中促销的购物项，不能为 {@code null}
     * @return 减免额合计；一项都没命中时为 0
     */
    private BigDecimal sumDiscount(List<PromotionDiscountItemVo> promotionItems) {
        BigDecimal total = BigDecimal.ZERO;
        for (PromotionDiscountItemVo item : promotionItems) {
            total = total.add(item.getDiscountAmount());
        }
        return total;
    }

    /**
     * 把促销减免转成 skuId 到减免额的映射。
     *
     * @param promotionItems 命中促销的购物项，不能为 {@code null}
     * @return skuId 到减免额的映射；没有命中的 SKU 不在里面，不返回 {@code null}
     */
    private Map<Long, BigDecimal> promotionBySkuId(List<PromotionDiscountItemVo> promotionItems) {
        Map<Long, BigDecimal> bySkuId = new HashMap<>();
        for (PromotionDiscountItemVo item : promotionItems) {
            bySkuId.put(item.getSkuId(), item.getDiscountAmount());
        }
        return bySkuId;
    }

    /**
     * 算一个购物项在促销之后的小计。
     *
     * @param item 购物项，{@code price} 与 {@code count} 不能为 {@code null}
     * @param promotionBySkuId 各购物项的促销减免额，不能为 {@code null}
     * @return 小计减促销减免；减到负数时按 0
     */
    private BigDecimal afterPromotion(OrderItemVo item, Map<Long, BigDecimal> promotionBySkuId) {
        BigDecimal promotion = promotionBySkuId.get(item.getSkuId());
        BigDecimal net = itemAmount(item).subtract(promotion == null ? BigDecimal.ZERO : promotion);
        return net.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : net;
    }

    /**
     * 把促销减免写到订单项上，并重算实付金额。
     *
     * <p>必须写到订单项而不是只记在订单头上：{@code computePrice} 把订单头的优惠金额算成
     * 各订单项 {@code promotionAmount} 的汇总，只写订单头会被那次汇总覆盖成 0。
     *
     * @param orderItems 待落库的订单项，不能为 {@code null}
     * @param promotionItems 各购物项命中的促销减免，不能为 {@code null}，可以为空
     */
    private void writePromotionToOrderItems(List<OrderItemEntity> orderItems,
                                            List<PromotionDiscountItemVo> promotionItems) {
        if (promotionItems.isEmpty()) {
            return;
        }
        Map<Long, BigDecimal> discountBySkuId = promotionBySkuId(promotionItems);

        for (OrderItemEntity orderItem : orderItems) {
            BigDecimal discount = discountBySkuId.get(orderItem.getSkuId());
            if (discount == null) {
                continue;
            }
            orderItem.setPromotionAmount(discount);
            // 实付金额在 builderOrderItem 里按"没有促销"算过一次，必须在这里重算：
            // 没用券也没用积分的订单不会再有别的写入点来纠正它
            orderItem.setRealAmount(originAmount(orderItem)
                    .subtract(orderItem.getCouponAmount())
                    .subtract(orderItem.getPromotionAmount())
                    .subtract(orderItem.getIntegrationAmount()));
        }
    }

    /* ═══════════════════ 优惠券 ═══════════════════ */

    /**
     * 按选中券把优惠并进结算金额。
     *
     * <p>确认页与换地址两条路径共用本方法：两处都必须减去同一个抵扣额。换地址只该覆盖运费，
     * 少了这一步，前端换完地址拿到的应付金额会退回原价，提交时被判成"商品价格已变动"。
     *
     * <p>改写入参 {@code fare} 的 {@code couponAmount}、{@code couponItems} 与 {@code payAmount}，
     * 商品总额与运费不动。
     *
     * @param fare 已算好商品总额与运费的结果，不能为 {@code null}
     * @param memberId 会员主键，不能为 {@code null}
     * @param cartItems 换算成券服务口径的购物项，不能为 {@code null}
     * @param couponHistoryId 选中的领取记录 ID；不用券时为 {@code null}
     * @return 选中券的抵扣结果；不用券时为 {@code null}
     * @throws BaseException 指定的券不可用，或券服务不可用时抛出
     */
    private CouponUsableVo applyCoupon(FareVo fare, Long memberId,
                                       List<CouponCartItemVo> cartItems, Long couponHistoryId) {
        CouponUsableVo selected = couponHistoryId == null
                ? null
                : requireCouponDiscount(memberId, cartItems, couponHistoryId);

        BigDecimal couponAmount = selected == null ? BigDecimal.ZERO : selected.getDiscountAmount();
        fare.setCouponAmount(couponAmount);
        fare.setCouponItems(splitCouponDiscount(cartItems, selected));
        fare.setPayAmount(fare.getPayAmount().subtract(couponAmount));
        return selected;
    }

    /**
     * 列出给定购物车可用的券。
     *
     * <p>查不到时返回空列表而不是抛异常：结算页的核心是地址、商品与金额，券列表只是其中一栏，
     * 它拿不到不该让整页打不开。会员看到的是"没有可用券"，而不是页面报错。
     * 这个降级是安全的 —— 它只影响展示，不影响任何金额。
     *
     * @param memberId 会员主键，不能为 {@code null}
     * @param cartItems 换算成券服务口径的购物项，不能为 {@code null}
     * @return 可用券列表；购物车为空或券服务不可用时为空列表，不返回 {@code null}
     */
    private List<CouponUsableVo> listUsableCoupons(Long memberId, List<CouponCartItemVo> cartItems) {
        if (cartItems.isEmpty()) {
            return List.of();
        }

        R<List<CouponUsableVo>> response;
        try {
            response = couponFeignService.available(memberId, cartItems);
        } catch (Exception e) {
            log.error("查询可用优惠券失败，memberId={}", memberId, e);
            return List.of();
        }
        if (response == null || response.getCode() != 0 || response.getData() == null) {
            log.error("查询可用优惠券失败，memberId={}, code={}", memberId,
                    response == null ? null : response.getCode());
            return List.of();
        }
        return response.getData();
    }

    /**
     * 取指定券在给定购物车下的抵扣额。
     *
     * <p>失败关闭而不是按不用券继续：抵扣额要并进应付金额，静默降级会让会员以为用上了券、
     * 实际按原价付了款。券服务把业务校验失败也包在 {@code code} 里返回，
     * 这里把码与文案原样转成异常，会员才能看到"未达到使用门槛"这类具体原因。
     *
     * @param memberId 会员主键，不能为 {@code null}
     * @param cartItems 换算成券服务口径的购物项，不能为 {@code null}
     * @param couponHistoryId 领取记录主键，不能为 {@code null}
     * @return 该券的抵扣结果，含总额与覆盖的 SKU，不会返回 {@code null}
     * @throws BaseException 券不可用，或券服务不可用时抛出
     */
    private CouponUsableVo requireCouponDiscount(Long memberId,
                                                 List<CouponCartItemVo> cartItems, Long couponHistoryId) {
        R<CouponUsableVo> response;
        try {
            response = couponFeignService.discount(memberId, couponHistoryId, cartItems);
        } catch (Exception e) {
            log.error("计算优惠券抵扣额失败，memberId={}, couponHistoryId={}", memberId, couponHistoryId, e);
            throw new BaseException(BaseCodeEnum.ORDER_COUPON_UNAVAILABLE);
        }
        if (response == null || response.getCode() != 0 || response.getData() == null) {
            throw new BaseException(
                    response == null ? BaseCodeEnum.ORDER_COUPON_UNAVAILABLE.getCode() : response.getCode(),
                    response == null ? BaseCodeEnum.ORDER_COUPON_UNAVAILABLE.getMsg() : response.getMsg());
        }
        return response.getData();
    }

    /**
     * 把券的抵扣额按商品小计比例摊到券覆盖的商品上。
     *
     * <p>只在 {@code coupon.scopeSkuIds} 之间摊：范围外的商品不能被这张券减掉，摊上优惠会让后续
     * 按订单项退款时退错钱。每项按比例向下取到分，最后一项吃掉舍入差额，
     * 保证各项之和恰好等于抵扣额。
     *
     * <p>分母用券服务给出的 {@code scopeAmount} 而不是重新求和：两边算的是同一批商品，
     * 重算一次只会多一个分叉点。
     *
     * <p>分子用促销后小计：{@code scopeAmount} 也是按促销后金额累计的，两处基数必须一致，
     * 否则各项之和会与抵扣额对不上。
     *
     * @param cartItems 换算成券服务口径的购物项，不能为 {@code null}
     * @param coupon 券的抵扣结果；不用券时为 {@code null}
     * @return 按 SKU 的抵扣拆分，各项之和恒等于抵扣额；不用券或一项都没覆盖时为空列表
     */
    private List<CouponDiscountItemVo> splitCouponDiscount(List<CouponCartItemVo> cartItems,
                                                           CouponUsableVo coupon) {
        if (coupon == null || coupon.getScopeSkuIds() == null || coupon.getScopeSkuIds().isEmpty()) {
            return List.of();
        }

        Set<Long> scopeSkuIds = new HashSet<>(coupon.getScopeSkuIds());
        List<CouponCartItemVo> scoped = cartItems.stream()
                .filter(item -> scopeSkuIds.contains(item.getSkuId()))
                .toList();
        BigDecimal scopeAmount = coupon.getScopeAmount() == null ? BigDecimal.ZERO : coupon.getScopeAmount();
        BigDecimal discount = coupon.getDiscountAmount() == null ? BigDecimal.ZERO : coupon.getDiscountAmount();
        if (scoped.isEmpty() || scopeAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return List.of();
        }

        List<CouponDiscountItemVo> shares = new ArrayList<>(scoped.size());
        BigDecimal allocated = BigDecimal.ZERO;
        for (int i = 0; i < scoped.size(); i++) {
            CouponCartItemVo item = scoped.get(i);
            BigDecimal share;
            if (i == scoped.size() - 1) {
                // 最后一项吃掉前面的舍入差额，各项之和才等于抵扣额
                share = discount.subtract(allocated);
            } else {
                share = discount.multiply(item.netSubtotal()).divide(scopeAmount, 2, RoundingMode.DOWN);
                allocated = allocated.add(share);
            }

            CouponDiscountItemVo shareVo = new CouponDiscountItemVo();
            shareVo.setSkuId(item.getSkuId());
            shareVo.setDiscountAmount(share);
            shares.add(shareVo);
        }
        return shares;
    }

    /**
     * 把券的抵扣拆分写到订单项上，并重算实付金额。
     *
     * <p>必须写到订单项而不是只记在订单头上：{@code computePrice} 把订单头的优惠金额算成
     * 各订单项 {@code couponAmount} 的汇总，只写订单头会被那次汇总覆盖成 0。
     *
     * @param orderItems 待落库的订单项，不能为 {@code null}
     * @param couponShares 按 SKU 的抵扣拆分，不能为 {@code null}，可以为空
     */
    private void writeCouponToOrderItems(List<OrderItemEntity> orderItems, List<CouponDiscountItemVo> couponShares) {
        if (couponShares.isEmpty()) {
            return;
        }
        Map<Long, BigDecimal> discountBySkuId = couponShares.stream()
                .collect(Collectors.toMap(CouponDiscountItemVo::getSkuId, CouponDiscountItemVo::getDiscountAmount,
                        (first, second) -> first));

        for (OrderItemEntity orderItem : orderItems) {
            BigDecimal discount = discountBySkuId.get(orderItem.getSkuId());
            if (discount == null) {
                continue;
            }
            orderItem.setCouponAmount(discount);
            // 实付金额在 builderOrderItem 里已算过一次，券额变了必须重算
            orderItem.setRealAmount(originAmount(orderItem)
                    .subtract(orderItem.getCouponAmount())
                    .subtract(orderItem.getPromotionAmount())
                    .subtract(orderItem.getIntegrationAmount()));
        }
    }

    /**
     * 把购物项换算成券服务认的形状。
     *
     * <p>只带 skuId、单价、数量与促销减免：SPU 与分类由券服务回查商品服务补齐，
     * 两边各维护一份"skuId 怎么变成分类"的逻辑迟早会分叉。
     *
     * <p>促销减免由本服务算好后带过去：券要按促销后金额判门槛，而券服务只认规则、不算钱。
     *
     * @param cartItems 已勾选的购物项，不能为 {@code null}
     * @param promotionItems 各购物项命中的促销减免，不能为 {@code null}，可以为空
     * @return 券服务认的购物项列表
     */
    private List<CouponCartItemVo> toCouponCartItems(List<OrderItemVo> cartItems,
                                                     List<PromotionDiscountItemVo> promotionItems) {
        Map<Long, BigDecimal> promotionBySkuId = promotionBySkuId(promotionItems);
        return cartItems.stream().map(item -> {
            CouponCartItemVo vo = new CouponCartItemVo();
            vo.setSkuId(item.getSkuId());
            vo.setPrice(item.getPrice());
            vo.setCount(item.getCount());
            vo.setPromotionAmount(promotionBySkuId.get(item.getSkuId()));
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 下单占用优惠券，并登记"事务回滚时释放"的补偿。
     *
     * <p>占用写在券服务自己的事务里，本事务回滚不会把它一起回滚。两道防线：
     * 一是把占用放在流程最后一步，让可能失败的步骤都已经过去；二是登记回滚补偿，
     * 提交失败时把券退回未使用。
     *
     * <p>补偿本身也可能失败，那种情况下券会停在"占用中"。这个方向的损失落在会员身上、
     * 且被券的有效期兜住，所以只记日志不抛出 —— 在回滚路径上再抛异常只会盖掉真正的失败原因。
     *
     * @param memberId 会员主键，不能为 {@code null}
     * @param order 已落库的订单，{@code id} 与 {@code orderSn} 必须已有值
     * @param couponHistoryId 领取记录主键；不用券时为 {@code null}
     * @throws BaseException 券已被别的订单占用、已过有效期、状态不对，或券服务不可用时抛出
     */
    private void lockCoupon(Long memberId, OrderEntity order, Long couponHistoryId) {
        if (couponHistoryId == null) {
            return;
        }

        R<Void> response;
        try {
            response = couponFeignService.lock(couponHistoryId, memberId, order.getId(), order.getOrderSn());
        } catch (Exception e) {
            log.error("占用优惠券失败，orderSn={}, couponHistoryId={}", order.getOrderSn(), couponHistoryId, e);
            throw new BaseException(BaseCodeEnum.ORDER_COUPON_UNAVAILABLE);
        }
        if (response == null || response.getCode() != 0) {
            throw new BaseException(
                    response == null ? BaseCodeEnum.ORDER_COUPON_UNAVAILABLE.getCode() : response.getCode(),
                    response == null ? BaseCodeEnum.ORDER_COUPON_UNAVAILABLE.getMsg() : response.getMsg());
        }

        String orderSn = order.getOrderSn();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    unlockCouponQuietly(orderSn);
                }
            }
        });
    }

    /**
     * 回滚补偿：把券退回未使用，失败只记日志。
     *
     * @param orderSn 订单号，不能为 {@code null}
     */
    private void unlockCouponQuietly(String orderSn) {
        try {
            couponFeignService.unlock(orderSn);
        } catch (Exception e) {
            log.error("回滚时释放优惠券失败，该券会停在占用中，orderSn={}", orderSn, e);
        }
    }

    /**
     * 关单时退回优惠券。
     *
     * <p>不吞异常：券留在"占用中"会让会员既用不了这张券、也拿不回抵扣额，与
     * "关了单却不释放库存"是同一类错误。抛出去让整个关单回滚，由关单消息重投兜底。
     *
     * @param orderSn 订单号，不能为 {@code null}
     * @throws BaseException 券服务不可用时抛出
     */
    private void unlockCoupon(String orderSn) {
        R<Void> response;
        try {
            response = couponFeignService.unlock(orderSn);
        } catch (Exception e) {
            log.error("退回优惠券失败，orderSn={}", orderSn, e);
            throw new BaseException(BaseCodeEnum.ORDER_COUPON_UNAVAILABLE);
        }
        if (response == null || response.getCode() != 0) {
            throw new BaseException(
                    response == null ? BaseCodeEnum.ORDER_COUPON_UNAVAILABLE.getCode() : response.getCode(),
                    response == null ? BaseCodeEnum.ORDER_COUPON_UNAVAILABLE.getMsg() : response.getMsg());
        }
    }

    /**
     * 支付成功时核销优惠券。
     *
     * <p>不吞异常：支付平台收到非成功应答会重新通知，而券服务的核销是幂等的，
     * 重试安全。反过来吞掉异常会留下一张永远停在"占用中"的券。
     *
     * <p>调用位置在"改订单状态"之前：这样核销失败时订单还没被置为已付款，
     * 重投能重新走一遍；先改状态的话重投会撞上"订单已支付"的幂等分支，核销就永远补不上了。
     *
     * @param orderSn 订单号，不能为 {@code null}
     * @throws BaseException 券服务不可用时抛出
     */
    private void consumeCoupon(String orderSn) {
        R<Void> response;
        try {
            response = couponFeignService.use(orderSn);
        } catch (Exception e) {
            log.error("核销优惠券失败，orderSn={}", orderSn, e);
            throw new BaseException(BaseCodeEnum.ORDER_COUPON_UNAVAILABLE);
        }
        if (response == null || response.getCode() != 0) {
            throw new BaseException(
                    response == null ? BaseCodeEnum.ORDER_COUPON_UNAVAILABLE.getCode() : response.getCode(),
                    response == null ? BaseCodeEnum.ORDER_COUPON_UNAVAILABLE.getMsg() : response.getMsg());
        }
    }

    /* ═══════════════════ 积分 ═══════════════════ */

    /**
     * 查询会员的积分余额，查不到按 0 返回。
     *
     * <p>余额只决定结算页上"积分抵扣"那一栏显不显示，不并进任何金额，所以拿不到时按 0 降级：
     * 会员看到的是"这单用不了积分"，而不是整页打不开。
     *
     * <p>代价是真正的余额会暂时不可见，所以它只用在算价路径；要凭余额决定"能不能扣"的那一步走
     * {@link #requireIntegrationBalance}。
     *
     * @param memberId 会员主键，不能为 {@code null}
     * @return 可用积分，最小为 0
     */
    private int optionalIntegrationBalance(Long memberId) {
        R<Integer> response;
        try {
            response = memberFeignService.integrationBalance(memberId);
        } catch (Exception e) {
            log.warn("查询积分余额失败，本次按 0 处理：memberId={}", memberId, e);
            return 0;
        }
        if (response == null || response.getCode() != 0 || response.getData() == null) {
            log.warn("查询积分余额返回非成功码，本次按 0 处理：memberId={}, code={}",
                    memberId, response == null ? null : response.getCode());
            return 0;
        }
        return response.getData();
    }

    /**
     * 查询会员的积分余额，取不到就报错。
     *
     * <p>用在"余额本身就是一道闸门"的地方 —— 下单扣减前校验够不够。那里按 0 降级会把会员服务的故障
     * 误报成"积分余额不足"，会员只会去查自己的积分，所以必须失败关闭。
     *
     * @param memberId 会员主键，不能为 {@code null}
     * @return 可用积分，最小为 0
     * @throws BaseException 会员服务不可用时抛 {@code ORDER_INTEGRATION_UNAVAILABLE}；
     *         会员不存在这类业务失败时原样转发 member 的错误码
     */
    private int requireIntegrationBalance(Long memberId) {
        R<Integer> response;
        try {
            response = memberFeignService.integrationBalance(memberId);
        } catch (Exception e) {
            log.error("查询积分余额失败，memberId={}", memberId, e);
            throw new BaseException(BaseCodeEnum.ORDER_INTEGRATION_UNAVAILABLE);
        }
        if (response == null) {
            throw new BaseException(BaseCodeEnum.ORDER_INTEGRATION_UNAVAILABLE);
        }
        if (response.getCode() != 0 || response.getData() == null) {
            throw new BaseException(response.getCode(), response.getMsg());
        }
        return response.getData();
    }

    /**
     * 把积分抵扣并进结算金额，并算出本单的可用上限。
     *
     * <p>必须在券之后调用：上限算的是"商品总额减促销减免再减券抵扣"的一半，顺序反了会多抵一笔。
     *
     * <p>改写入参 {@code fare} 的 {@code integrationAmount}、{@code integrationItems} 与
     * {@code payAmount}；商品总额、运费与券优惠都不动。
     *
     * @param fare 已算好商品总额、运费与券优惠的结果，不能为 {@code null}
     * @param cartItems 已勾选的购物项，用于逐项分摊，不能为 {@code null}
     * @param balance 会员积分余额
     * @param useIntegration 请求使用的积分数；{@code null} 或 0 表示不用
     * @return 本单最多可用的积分数；为 0 表示本单用不了积分
     * @throws BaseException 请求数超过本单上限（15010）或超过余额（15009）时抛出
     */
    private int applyIntegration(FareVo fare, List<OrderItemVo> cartItems, int balance, Integer useIntegration) {
        // 封顶按"商品总额减促销减免再减券抵扣"算：积分抵的是会员实际要付的那部分
        BigDecimal goodsAfterPromotion = fare.getTotalAmount().subtract(fare.getPromotionAmount());
        int limit = IntegrationConstant.maxUsablePoints(goodsAfterPromotion, fare.getCouponAmount(), balance);
        int points = resolveIntegrationPoints(limit, balance, useIntegration);

        BigDecimal amount = points == 0 ? BigDecimal.ZERO : IntegrationConstant.pointsToAmount(points);
        fare.setIntegrationAmount(amount);
        fare.setIntegrationItems(points == 0
                ? List.of()
                : splitIntegration(amount, cartItems, promotionBySkuId(fare.getPromotionItems())));
        fare.setPayAmount(fare.getPayAmount().subtract(amount));
        return limit;
    }

    /**
     * 校验请求使用的积分数，返回实际使用的数量。
     *
     * <p>不做静默夹取：请求数超过上限或余额时直接报错。夹取会改掉应付金额，让前端回传的价格对不上，
     * 最后落到一个说不清的"价格已变动"。
     *
     * @param limit 本单最多可用的积分数，由调用方按订单金额算出
     * @param balance 会员积分余额
     * @param useIntegration 请求使用的积分数，可以为 {@code null}
     * @return 实际使用的积分数，最小为 0
     * @throws BaseException 数量为负数（10001）、超过余额（15009）或超过本单上限（15010）时抛出
     */
    private int resolveIntegrationPoints(int limit, int balance, Integer useIntegration) {
        int points = useIntegration == null ? 0 : useIntegration;
        if (points < 0) {
            throw new BaseException(BaseCodeEnum.VALID_EXCEPTION, "使用的积分数不能为负数");
        }
        if (points > balance) {
            throw new BaseException(BaseCodeEnum.INTEGRATION_NOT_ENOUGH);
        }
        if (points > limit) {
            // 上限依赖本单金额，只有这里算得出来，所以具体数值在这里覆盖 msg
            throw new BaseException(BaseCodeEnum.INTEGRATION_LIMIT_EXCEEDED,
                    "本单最多可用 " + limit + " 积分，请调整后重试");
        }
        return points;
    }

    /**
     * 把积分抵扣额摊到各购物项上。
     *
     * <p>积分没有适用范围，基数是全部购物项<b>促销后</b>金额之和；最后一项吃掉前面的舍入差额，
     * 各项之和恒等于抵扣额。用促销后金额而不是原价：封顶上限也是按促销后金额算的，
     * 两处基数不同会让某一项分到的抵扣额超过它自己的应付额。
     *
     * @param amount 抵扣额，不能为 {@code null}，不能为负数
     * @param cartItems 已勾选的购物项，不能为 {@code null}
     * @param promotionBySkuId 各购物项的促销减免额，不能为 {@code null}；没有命中的 SKU 不在里面
     * @return 按 SKU 的抵扣拆分；一项都没有或促销后总额为 0 时为空列表
     */
    private List<IntegrationDiscountItemVo> splitIntegration(BigDecimal amount, List<OrderItemVo> cartItems,
                                                            Map<Long, BigDecimal> promotionBySkuId) {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemVo item : cartItems) {
            total = total.add(afterPromotion(item, promotionBySkuId));
        }
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            return List.of();
        }

        List<IntegrationDiscountItemVo> shares = new ArrayList<>(cartItems.size());
        BigDecimal allocated = BigDecimal.ZERO;
        for (int i = 0; i < cartItems.size(); i++) {
            OrderItemVo item = cartItems.get(i);
            BigDecimal share;
            if (i == cartItems.size() - 1) {
                share = amount.subtract(allocated);
            } else {
                share = amount.multiply(afterPromotion(item, promotionBySkuId))
                        .divide(total, 2, RoundingMode.DOWN);
                allocated = allocated.add(share);
            }

            IntegrationDiscountItemVo shareVo = new IntegrationDiscountItemVo();
            shareVo.setSkuId(item.getSkuId());
            shareVo.setDiscountAmount(share);
            shares.add(shareVo);
        }
        return shares;
    }

    /**
     * 算单个购物项的金额，即单价 × 数量。
     *
     * <p>不读 {@code totalPrice}：那是购物车下发的展示字段，结算链路只保证 {@code price} 与
     * {@code count} 有值。算法与 {@link #sumItemAmount} 一致，分摊的基数与汇总才不会差几分钱。
     *
     * @param item 购物项，{@code price} 与 {@code count} 不能为 {@code null}
     * @return 该项的金额
     */
    private BigDecimal itemAmount(OrderItemVo item) {
        return item.getPrice().multiply(BigDecimal.valueOf(item.getCount()));
    }

    /**
     * 把积分抵扣的拆分写到订单项上，并重算实付金额。
     *
     * <p>必须写到订单项而不是只记在订单头上：{@code computePrice} 把订单头的积分抵扣额算成
     * 各订单项 {@code integrationAmount} 的汇总，只写订单头会被那次汇总覆盖成 0。
     *
     * @param orderItems 待落库的订单项，不能为 {@code null}
     * @param shares 按 SKU 的抵扣拆分，不能为 {@code null}，可以为空
     */
    private void writeIntegrationToOrderItems(List<OrderItemEntity> orderItems,
                                              List<IntegrationDiscountItemVo> shares) {
        if (shares.isEmpty()) {
            return;
        }
        Map<Long, BigDecimal> amountBySkuId = shares.stream()
                .collect(Collectors.toMap(IntegrationDiscountItemVo::getSkuId,
                        IntegrationDiscountItemVo::getDiscountAmount, (first, second) -> first));

        for (OrderItemEntity orderItem : orderItems) {
            BigDecimal amount = amountBySkuId.get(orderItem.getSkuId());
            if (amount == null) {
                continue;
            }
            orderItem.setIntegrationAmount(amount);
            // 实付金额在 builderOrderItem 里已算过一次，积分额变了必须重算
            orderItem.setRealAmount(originAmount(orderItem)
                    .subtract(orderItem.getCouponAmount())
                    .subtract(orderItem.getPromotionAmount())
                    .subtract(orderItem.getIntegrationAmount()));
        }
    }

    /**
     * 下单抵扣积分，并登记"事务回滚时退回"的补偿。
     *
     * <p>抵扣写在会员服务自己的事务里，本事务回滚不会把它一起回滚，所以要在调用之前就登记回滚补偿：
     * 抵扣已经落账、应答却在回程丢失时，本端会抛异常并回滚，补偿是会员拿回那部分积分的唯一机会。
     * 抵扣根本没发生时，退回在 member 侧找不到对应的抵扣流水，按无操作处理，不会凭空加分。
     *
     * <p>补偿本身也可能失败，那种情况下积分会停在已抵扣。这个方向的损失落在会员身上，
     * 所以只记日志不抛出 —— 在回滚路径上再抛异常只会盖掉真正的失败原因。
     *
     * @param memberId 会员主键，不能为 {@code null}
     * @param orderSn 订单号，不能为 {@code null}
     * @param points 抵扣的积分数；{@code null} 或非正数表示本单没用积分，直接返回
     * @throws BaseException 余额不足、会员不存在或会员服务不可用时抛出
     */
    private void deductIntegration(Long memberId, String orderSn, Integer points) {
        if (points == null || points <= 0) {
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    refundIntegrationQuietly(memberId, orderSn, points);
                }
            }
        });

        R<Void> response;
        try {
            response = memberFeignService.deductIntegration(memberId, orderSn, points);
        } catch (Exception e) {
            log.error("抵扣积分失败，orderSn={}, points={}", orderSn, points, e);
            throw new BaseException(BaseCodeEnum.ORDER_INTEGRATION_UNAVAILABLE);
        }
        if (response == null || response.getCode() != 0) {
            // 原样转发 member 的码：余额不足（15009）要直接告诉会员，不能笼统报"服务不可用"，
            // 否则会员只会反复重试
            throw new BaseException(
                    response == null ? BaseCodeEnum.ORDER_INTEGRATION_UNAVAILABLE.getCode() : response.getCode(),
                    response == null ? BaseCodeEnum.ORDER_INTEGRATION_UNAVAILABLE.getMsg() : response.getMsg());
        }
    }

    /**
     * 回滚补偿：把已抵扣的积分退回，失败只记日志。
     *
     * @param memberId 会员主键，不能为 {@code null}
     * @param orderSn 订单号，不能为 {@code null}
     * @param points 退回的积分数
     */
    private void refundIntegrationQuietly(Long memberId, String orderSn, Integer points) {
        try {
            memberFeignService.refundIntegration(memberId, orderSn, points);
        } catch (Exception e) {
            log.error("回滚时退回积分失败，这部分积分会停在已抵扣，orderSn={}, points={}", orderSn, points, e);
        }
    }

    /**
     * 关单时退回已抵扣的积分。
     *
     * <p>不吞异常：积分丢了会让会员既没买到东西也没拿回积分，与"关了单却不释放库存"是同一类错误。
     * 抛出去让整个关单回滚，由关单消息重投兜底。
     *
     * @param memberId 会员主键，不能为 {@code null}
     * @param orderSn 订单号，不能为 {@code null}
     * @param points 退回的积分数；{@code null} 或非正数表示本单没用积分，直接返回
     * @throws BaseException 会员服务不可用时抛出
     */
    private void refundIntegration(Long memberId, String orderSn, Integer points) {
        if (points == null || points <= 0) {
            return;
        }

        R<Void> response;
        try {
            response = memberFeignService.refundIntegration(memberId, orderSn, points);
        } catch (Exception e) {
            log.error("退回积分失败，orderSn={}, points={}", orderSn, points, e);
            throw new BaseException(BaseCodeEnum.ORDER_INTEGRATION_UNAVAILABLE);
        }
        if (response == null || response.getCode() != 0) {
            throw new BaseException(
                    response == null ? BaseCodeEnum.ORDER_INTEGRATION_UNAVAILABLE.getCode() : response.getCode(),
                    response == null ? BaseCodeEnum.ORDER_INTEGRATION_UNAVAILABLE.getMsg() : response.getMsg());
        }
    }

    /**
     * 支付成功时赠送积分。
     *
     * <p>不吞异常：支付平台收到非成功应答会重新通知，而赠送是幂等的，重试安全；
     * 反过来吞掉异常会让会员白白少拿积分，且没有任何地方会再补一次。
     *
     * @param memberId 会员主键，不能为 {@code null}
     * @param orderSn 订单号，不能为 {@code null}
     * @param points 赠送的积分数；{@code null} 或非正数表示这一单没有可得积分，直接返回
     * @throws BaseException 会员服务不可用时抛出
     */
    private void grantIntegration(Long memberId, String orderSn, Integer points) {
        if (points == null || points <= 0) {
            return;
        }

        R<Void> response;
        try {
            response = memberFeignService.grantIntegration(memberId, orderSn, points);
        } catch (Exception e) {
            log.error("赠送积分失败，orderSn={}, points={}", orderSn, points, e);
            throw new BaseException(BaseCodeEnum.ORDER_INTEGRATION_UNAVAILABLE);
        }
        if (response == null || response.getCode() != 0) {
            throw new BaseException(
                    response == null ? BaseCodeEnum.ORDER_INTEGRATION_UNAVAILABLE.getCode() : response.getCode(),
                    response == null ? BaseCodeEnum.ORDER_INTEGRATION_UNAVAILABLE.getMsg() : response.getMsg());
        }
    }

    /**
     * 支付成功时赠送成长值。
     *
     * <p>不吞异常：支付平台收到非成功应答会重新通知，而赠送是幂等的，重试安全；
     * 反过来吞掉异常会让会员白白少拿成长值，且没有任何地方会再补一次。
     *
     * @param memberId 会员主键，不能为 {@code null}
     * @param orderSn 订单号，不能为 {@code null}
     * @param points 赠送的成长值数；{@code null} 或非正数表示这一单没有可得成长值，直接返回
     * @throws BaseException 会员成长值服务不可用时抛出
     */
    private void grantGrowth(Long memberId, String orderSn, Integer points) {
        if (points == null || points <= 0) {
            return;
        }

        R<Void> response;
        try {
            response = memberFeignService.grantGrowth(memberId, orderSn, points);
        } catch (Exception e) {
            log.error("赠送成长值失败，orderSn={}, points={}", orderSn, points, e);
            throw new BaseException(BaseCodeEnum.ORDER_GROWTH_UNAVAILABLE);
        }
        if (response == null || response.getCode() != 0) {
            throw new BaseException(
                    response == null ? BaseCodeEnum.ORDER_GROWTH_UNAVAILABLE.getCode() : response.getCode(),
                    response == null ? BaseCodeEnum.ORDER_GROWTH_UNAVAILABLE.getMsg() : response.getMsg());
        }
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
            // 评价摘要一次批量取：供卡片决定显示「评价」还是「查看评价」
            attachCommentBriefs(records);
            // 售后申请一次批量挂上：卡片要显示"退款中/退货中"，并按它决定申请入口显不显示
            attachAfterSales(records);
        }
        fillStatusText(records);

        page.setRecords(records);
        return new PageVO<>(page.getTotal(), page.getRecords());
    }

    /**
     * 给订单项挂上评价摘要，供会员端把已评价的入口显示成「查看评价」。
     *
     * <p>把这一页所有订单项 ID 一次交给 product，按订单项 ID 回挂 —— 逐个订单项查是 N+1，还是跨服务。
     * product 不可用时整页按"都没评价"处理并记 warn：评价摘要只决定按钮文案，取不到不该让订单列表打不开。
     * 代价是那一次按钮显示成「评价」，点下去会拿到 11203。</p>
     *
     * @param records 本页订单，不能为 {@code null}；订单项已填好
     */
    private void attachCommentBriefs(List<OrderEntity> records) {
        List<Long> orderItemIds = records.stream()
                .flatMap(order -> order.getOrderItemEntityList().stream())
                .map(OrderItemEntity::getId)
                .filter(id -> id != null)
                .toList();
        if (orderItemIds.isEmpty()) {
            return;
        }

        Map<Long, CommentBriefVo> briefs;
        try {
            R<List<CommentBriefVo>> result = productFeignService.listCommentedItems(orderItemIds);
            if (result == null || result.getCode() != R.SUCCESS_CODE || result.getData() == null) {
                log.warn("查询评价摘要失败，本次按未评价处理：orderItemIds={}, code={}",
                        orderItemIds.size(), result == null ? null : result.getCode());
                return;
            }
            briefs = result.getData().stream()
                    .collect(Collectors.toMap(CommentBriefVo::getOrderItemId, vo -> vo, (first, second) -> first));
        } catch (Exception e) {
            log.warn("查询评价摘要异常，本次按未评价处理：orderItemIds={}", orderItemIds.size(), e);
            return;
        }

        records.forEach(order -> order.getOrderItemEntityList()
                .forEach(item -> item.setComment(briefs.get(item.getId()))));
    }

    /** {@inheritDoc} */
    @Override
    public OrderEntity getOrderDetail(MemberResponseVo user, String orderSn) {
        return fillDetail(requireOwnOrder(user, orderSn));
    }

    /** {@inheritDoc} */
    @Override
    public OrderEntity getOrderDetailForAdmin(String orderSn) {
        OrderEntity order = getOrderByOrderSn(orderSn);
        if (order == null) {
            throw new BaseException(BaseCodeEnum.ORDER_NOT_FOUND);
        }
        fillDetail(order);
        order.setOperateHistoryList(listOperateHistory(order.getId()));
        order.setPaymentInfoList(listPaymentInfo(orderSn));
        return order;
    }

    /**
     * 装配订单详情：补订单项与状态文案。
     *
     * <p>只在详情接口用，列表不带订单项：一页十条订单各查一次订单项是 N+1。</p>
     *
     * @param order 已查出的订单，不能为 {@code null}
     * @return 同一个订单对象，{@code orderItemEntityList} 与 {@code statusText} 已填好
     */
    private OrderEntity fillDetail(OrderEntity order) {
        order.setOrderItemEntityList(orderItemService.list(
                new LambdaQueryWrapper<OrderItemEntity>().eq(OrderItemEntity::getOrderSn, order.getOrderSn())));
        order.setStatusText(statusText(order.getStatus()));
        // 详情也要带售后：会员在详情里看退货地址与进度，后台看这单走过哪些售后
        attachAfterSales(List.of(order));
        return order;
    }

    /**
     * 查一笔订单的操作流水，按操作时间升序，同一时刻的按主键升序。
     *
     * <p>状态文案随行填好：与订单列表、订单详情的文案出自同一处映射。</p>
     *
     * @param orderId 订单 ID，不能为 {@code null}
     * @return 操作流水；没有记录时为空列表
     */
    private List<OrderOperateHistoryEntity> listOperateHistory(Long orderId) {
        List<OrderOperateHistoryEntity> records = orderOperateHistoryService.list(
                new LambdaQueryWrapper<OrderOperateHistoryEntity>()
                        .eq(OrderOperateHistoryEntity::getOrderId, orderId)
                        .orderByAsc(OrderOperateHistoryEntity::getCreateTime)
                        .orderByAsc(OrderOperateHistoryEntity::getId));
        for (OrderOperateHistoryEntity record : records) {
            record.setStatusText(statusText(record.getOrderStatus()));
        }
        return records;
    }

    /**
     * 查一笔订单的支付流水，按写入顺序升序。
     *
     * <p>一笔订单可能有多条：重复的回调各写一条，所以返回列表而不是单条。</p>
     *
     * @param orderSn 订单号，不能为 {@code null}
     * @return 支付流水；没有记录时为空列表
     */
    private List<PaymentInfoEntity> listPaymentInfo(String orderSn) {
        return paymentInfoService.list(new LambdaQueryWrapper<PaymentInfoEntity>()
                .eq(PaymentInfoEntity::getOrderSn, orderSn)
                .orderByAsc(PaymentInfoEntity::getId));
    }

    /** {@inheritDoc} */
    @Override
    public OrderEntity getOrderByOrderSn(String orderSn) {
        return this.baseMapper.selectOne(new LambdaQueryWrapper<OrderEntity>().eq(OrderEntity::getOrderSn, orderSn));
    }



    /**
     * 为订单列表填充状态文案。
     *
     * <p>文案由后端给而不是前端自己映射：{@link OrderStatusEnum} 有 5 个值，
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

        if (PayConstant.MOCK.equals(payType)) {
            result.setCashierUrl(buildMockCashierUrl(orderSn));
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

    /**
     * 签发测试支付的一次性票据，并拼出收银台地址。
     *
     * <p>票据覆盖同一订单的上一张：同一订单重复发起支付时，只有最后点开的那张收银台能用。
     * 地址里带上金额与商品名，两者都只用于收银台展示 —— 商户侧的收款金额始终取库里的应付总额。
     *
     * @param orderSn 订单号，不能为 {@code null}
     * @return 收银台地址，前端在新窗口打开
     */
    private String buildMockCashierUrl(String orderSn) {
        String ticket = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(
                OrderConstant.MOCK_PAY_TICKET_PREFIX + orderSn,
                ticket,
                OrderConstant.MOCK_PAY_TICKET_TIMEOUT_MINUTES,
                TimeUnit.MINUTES);

        // 复用支付宝那条的参数组装：金额取库里的应付总额、商品名取第一个订单项
        PayVo payVo = buildPayVo(orderSn);
        return UriComponentsBuilder.fromUriString(mockCashierUrl)
                .queryParam("orderSn", payVo.getOut_trade_no())
                .queryParam("amount", payVo.getTotal_amount())
                .queryParam("subject", payVo.getSubject())
                .queryParam("ticket", ticket)
                .build()
                .encode()
                .toUriString();
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
        // 只挡明显不能取消的请求，真正的判据在 doClose 的条件更新里
        if (!OrderStatusEnum.CREATE_NEW.getCode().equals(order.getStatus())) {
            throw new BaseException(BaseCodeEnum.ORDER_STATUS_INVALID);
        }
        // 并发下订单可能在两次判断之间被支付成功，此时取消不生效，必须让调用方知道
        if (!doClose(order, user.getUsername(), "会员取消订单")) {
            throw new BaseException(BaseCodeEnum.ORDER_STATUS_INVALID);
        }
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
            doClose(orderInfo, "系统", "超时未支付，系统自动关闭");
        }
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeOrderByAdmin(String orderSn, String operator) {
        OrderEntity order = getOrderByOrderSn(orderSn);
        if (order == null) {
            throw new BaseException(BaseCodeEnum.ORDER_NOT_FOUND);
        }

        // 关单不生效只可能是订单已被支付或已关闭：doClose 带 status = 0 条件，
        // 没生效就没写过任何数据，报状态错误是安全的
        if (!doClose(order, operator, "后台关闭订单")) {
            throw new BaseException(BaseCodeEnum.ORDER_STATUS_INVALID);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void updateNote(String orderSn, String note, String operator) {
        // 先取订单：操作史要订单 ID，而更新语句只认订单号
        OrderEntity order = getOrderByOrderSn(orderSn);
        if (order == null) {
            throw new BaseException(BaseCodeEnum.ORDER_NOT_FOUND);
        }

        if (this.baseMapper.updateNote(orderSn, note) == 0) {
            throw new BaseException(BaseCodeEnum.ORDER_NOT_FOUND);
        }

        saveOperateHistory(order.getId(), order.getStatus(), operator,
                note == null ? "修改订单备注：清空" : "修改订单备注：" + note);
    }

    /** {@inheritDoc} */
    @Override
    public void deleteOrder(MemberResponseVo user, String orderSn) {
        // 先按归属取单：不属于自己的按"不存在"报，与其它会员接口一致
        OrderEntity order = requireOwnOrder(user, orderSn);

        // 状态与"未删除"的判断写进 UPDATE 的 WHERE：并发重复删除只有一个会生效
        if (this.baseMapper.markDeleted(orderSn, user.getId()) == 0) {
            throw new BaseException(BaseCodeEnum.ORDER_STATUS_INVALID);
        }

        saveOperateHistory(order.getId(), order.getStatus(), user.getUsername(), "会员删除订单（仅从会员列表隐藏）");
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void hardDeleteOrder(String orderSn, String operator) {
        OrderEntity order = getOrderByOrderSn(orderSn);
        if (order == null || !DELETED_STATUS.equals(order.getDeleteStatus())) {
            // 只删被会员标记过的订单：没标记过的单不提供物理删除的入口
            throw new BaseException(BaseCodeEnum.ORDER_NOT_FOUND);
        }

        // 先删仓库侧的工作单：那边在一个事务里校验"没有锁定中的明细"再删，被拒时本地一行都没动。
        // 反过来先删本地，远程失败会留下指向已删订单的工单，而订单已经查不到、无从重试
        R<Void> workOrder = wmsFeignService.deleteWorkOrderByOrderSn(orderSn);
        if (workOrder.getCode() != 0) {
            throw new BaseException(workOrder.getCode(), workOrder.getMsg());
        }

        orderItemService.remove(new LambdaQueryWrapper<OrderItemEntity>().eq(OrderItemEntity::getOrderSn, orderSn));
        paymentInfoService.remove(new LambdaQueryWrapper<PaymentInfoEntity>()
                .eq(PaymentInfoEntity::getOrderSn, orderSn));
        deleteReturnRecords(orderSn);
        // 该订单的操作流水一并删掉：订单都没了，留着指向不存在订单的记录只是垃圾
        orderOperateHistoryService.remove(new LambdaQueryWrapper<OrderOperateHistoryEntity>()
                .eq(OrderOperateHistoryEntity::getOrderId, order.getId()));

        // 准入条件再落一次到 SQL：没被标记过的单在这里也删不掉
        if (this.baseMapper.deleteMarked(orderSn) == 0) {
            throw new BaseException(BaseCodeEnum.ORDER_NOT_FOUND);
        }

        log.warn("后台硬删订单：orderSn={}, status={}, memberId={}, 操作人={}",
                orderSn, order.getStatus(), order.getMemberId(), operator);
    }

    /**
     * 写一条订单操作历史。
     *
     * <p>只在业务变更确实生效后调用：调用方一律用同一条 UPDATE 返回的受影响行数判断，
     * 重复回调、重复点击这类没改到任何行的请求不留记录。</p>
     *
     * <p>说明文字按 {@code note} 列上限截断：超长备注会让整条记录写失败，操作就留不下痕迹。</p>
     *
     * @param orderId 订单 ID，不能为 {@code null}
     * @param status 操作之后订单处于的状态，取值见 {@link OrderStatusEnum}
     * @param operateMan 操作人：会员或管理员的用户名；系统动作用"系统"
     * @param note 操作说明，不能为 {@code null}
     */
    private void saveOperateHistory(Long orderId, Integer status, String operateMan, String note) {
        OrderOperateHistoryEntity history = new OrderOperateHistoryEntity();
        history.setOrderId(orderId);
        history.setOperateMan(operateMan);
        history.setCreateTime(new Date());
        history.setOrderStatus(status);
        history.setNote(note.length() > NOTE_MAX_LENGTH ? note.substring(0, NOTE_MAX_LENGTH) : note);
        orderOperateHistoryService.save(history);
    }

    /**
     * 删除订单的售后记录：退货申请与它对应的退款流水。
     *
     * <p>退款流水只按退货申请 ID 关联，所以先查出申请的 ID 再删流水，两步顺序不能颠倒。</p>
     *
     * @param orderSn 订单号，不能为 {@code null}
     */
    private void deleteReturnRecords(String orderSn) {
        List<OrderReturnApplyEntity> applies = orderReturnApplyService.list(
                new LambdaQueryWrapper<OrderReturnApplyEntity>().eq(OrderReturnApplyEntity::getOrderSn, orderSn));
        if (applies.isEmpty()) {
            return;
        }

        List<Long> applyIds = applies.stream().map(OrderReturnApplyEntity::getId).toList();
        refundInfoService.remove(new LambdaQueryWrapper<RefundInfoEntity>()
                .in(RefundInfoEntity::getOrderReturnId, applyIds));
        orderReturnApplyService.removeByIds(applyIds);
    }

    /**
     * 关单：置为已取消，通知仓库释放库存，并退回优惠券。
     *
     * <p>超时关单（MQ）、用户主动取消与后台关单共用这一段。本方法自己不承担事务边界 —— 它是私有方法，
     * 注解在此不生效；边界由三个调用者 {@code cancelOrder}、{@code closeOrder} 与
     * {@code closeOrderByAdmin} 承担，保证"置为已取消""释放库存的消息记录"与"退回优惠券"一起生效。
     *
     * <p>并发：关单与支付回调谁先到谁赢。订单已被支付时关单不生效，此时不释放库存也不退券。
     *
     * @param orderInfo 待关闭的订单
     * @param operateMan 操作人：会员或管理员的用户名；超时关单用"系统"
     * @param note 写进操作史的说明
     * @return {@code true} 表示确实关掉了；{@code false} 表示订单状态已被并发变更，未做任何变更
     */
    private boolean doClose(OrderEntity orderInfo, String operateMan, String note) {

        // 判断与写入是同一条 SQL：与支付回调并发时谁先到谁赢
        int affected = this.baseMapper.closeOrder(orderInfo.getOrderSn());
        if (affected == 0) {
            // 状态没改成：订单其实已被付款，库存该被实扣而不是释放
            log.warn("关单未生效，订单状态已被并发变更（可能是刚支付成功）：orderSn={}", orderInfo.getOrderSn());
            return false;
        }

        saveOperateHistory(orderInfo.getId(), OrderStatusEnum.CANCLED.getCode(), operateMan, note);

        OrderTo orderTo = new OrderTo();
        BeanUtils.copyProperties(orderInfo, orderTo);
        // copyProperties 拷到的是关单前的对象，status 仍是待付款；ware 按库存工作单解锁、
        // 不读这个字段，但 OrderTo 是订单快照，发出去的状态必须与库里一致
        orderTo.setStatus(OrderStatusEnum.CANCLED.getCode());

        // 不吞异常：消息记录写不进去就整体回滚，宁可不关单，也不能关了单却不释放库存
        reliableMqPublisher.publish(MqConstant.Exchanges.ORDER, MqConstant.RoutingKeys.ORDER_CLOSED, orderTo);

        // 退回券与释放库存同一个道理，也不吞异常。没用券的订单不调券服务：
        // 那边虽然把"没有占用中的券"当无操作，但没必要让关单依赖券服务的可用性
        if (orderInfo.getCouponId() != null) {
            unlockCoupon(orderInfo.getOrderSn());
        }

        // 积分同理：退回失败会让会员白丢积分，所以不吞异常，由关单消息重投兜底。
        // 没用积分的订单不调会员服务
        refundIntegration(orderInfo.getMemberId(), orderInfo.getOrderSn(), orderInfo.getUseIntegration());

        return true;
    }

    /* ═══════════════════ 售后（仅退款 / 退货退款） ═══════════════════ */

    /** {@inheritDoc} */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public void applyRefund(MemberResponseVo user, RefundApplyVo vo) {
        // 归属先判：不属于自己的订单按"不存在"报，与其它会员接口一致
        requireOwnOrder(user, vo.getOrderSn());

        // 锁住订单行再判状态与重复：状态判断要在锁内做，否则并发下两个请求会各自看到"还没有申请"
        OrderEntity locked = lockOrderForAfterSale(vo.getOrderSn());

        // 只受理"已付款、还没发货"的订单：这一条要复用关单的解锁库存与退券退积分，
        // 而发货之后那些账都已经结掉了，那时该走退货
        if (!OrderStatusEnum.PAYED.getCode().equals(locked.getStatus())) {
            throw new BaseException(BaseCodeEnum.ORDER_STATUS_INVALID);
        }

        if (hasActiveAfterSale(locked.getOrderSn(), AfterSaleTypeEnum.REFUND.getCode(), null)) {
            throw new BaseException(BaseCodeEnum.ORDER_AFTER_SALE_APPLIED);
        }

        OrderReturnApplyEntity apply = new OrderReturnApplyEntity();
        apply.setOrderId(locked.getId());
        apply.setOrderSn(locked.getOrderSn());
        apply.setType(AfterSaleTypeEnum.REFUND.getCode());
        apply.setCreateTime(new Date());
        apply.setMemberUsername(locked.getMemberUsername());
        // 整单退：金额就是订单实付。下单时券与积分的逐项分摊已经算定，这里不重算、也不按项摊
        apply.setReturnAmount(locked.getPayAmount());
        // 退款不填原因：原因字典里的取值都是"收到货之后"的场景（质量问题、七天无理由、收到破损），
        // 硬套到"还没发货就不想要了"上会得到一批解释不通的报表。会员要说的话在说明里
        apply.setDescription(vo.getDescription());
        apply.setStatus(AfterSaleStatusEnum.PENDING.getCode());
        orderReturnApplyService.save(apply);

        saveOperateHistory(locked.getId(), locked.getStatus(), user.getUsername(),
                "会员申请退款：" + vo.getDescription());
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public void applyReturn(MemberResponseVo user, ReturnApplyVo vo) {
        // 入口是订单项：先按订单项定位订单，再判归属 —— 与评价那条链同一套顺序
        OrderItemEntity item = orderItemService.getById(vo.getOrderItemId());
        if (item == null) {
            throw new BaseException(BaseCodeEnum.ORDER_ITEM_NOT_FOUND);
        }
        requireOwnOrder(user, item.getOrderSn());

        OrderEntity locked = lockOrderForAfterSale(item.getOrderSn());

        // 确认收货之后才允许退货：货在会员手里，退的是具体某一项
        if (!OrderStatusEnum.RECIEVED.getCode().equals(locked.getStatus())) {
            throw new BaseException(BaseCodeEnum.ORDER_STATUS_INVALID);
        }

        if (hasActiveAfterSale(locked.getOrderSn(), AfterSaleTypeEnum.RETURN.getCode(), item.getSkuId())) {
            throw new BaseException(BaseCodeEnum.ORDER_AFTER_SALE_APPLIED);
        }

        OrderReturnApplyEntity apply = new OrderReturnApplyEntity();
        apply.setOrderId(locked.getId());
        apply.setOrderSn(locked.getOrderSn());
        apply.setType(AfterSaleTypeEnum.RETURN.getCode());
        apply.setCreateTime(new Date());
        apply.setMemberUsername(locked.getMemberUsername());
        // 整项退：金额取该项经过券与积分分摊后的实付额。不按数量摊 —— 不做部分数量退货，
        // 摊出来的分位差还需要一条"末项吃差额"的规则，而这里根本没有"末项"
        apply.setReturnAmount(item.getRealAmount());
        apply.setReason(vo.getReason());
        apply.setDescription(vo.getDescription());
        apply.setDescPics(vo.getDescPics());
        apply.setStatus(AfterSaleStatusEnum.PENDING.getCode());
        // 商品快照随申请留下：商品之后可能改价、下架，核对该退哪一件时按申请这一刻的信息
        apply.setSkuId(item.getSkuId());
        apply.setSkuName(item.getSkuName());
        apply.setSkuImg(item.getSkuPic());
        apply.setSkuBrand(item.getSpuBrand());
        apply.setSkuAttrsVals(item.getSkuAttrsVals());
        apply.setSkuCount(item.getSkuQuantity());
        apply.setSkuPrice(item.getSkuPrice());
        apply.setSkuRealPrice(realUnitPrice(item));
        // 退货人与电话取订单上的收货人：货当初就是寄给他的，退回时默认由他寄。
        // 再让会员填一遍，只是把订单上已有的信息重抄一次
        apply.setReturnName(locked.getReceiverName());
        apply.setReturnPhone(locked.getReceiverPhone());
        orderReturnApplyService.save(apply);

        saveOperateHistory(locked.getId(), locked.getStatus(), user.getUsername(),
                "会员申请退货：" + item.getSkuName());
    }

    /**
     * 算订单项的实付单价，只给页面看。
     *
     * <p>权威金额是 {@code real_amount}（整项实付），它已经吸收过券分摊的末位取舍；从它反推单价
     * 会带分位差，所以这一列只当展示用、不与 {@code return_amount} 相互校验。数量为空或非正时
     * 返回 {@code null} —— 算不出来就不编一个数（与"来源未实现就留 NULL"同一口径）。</p>
     *
     * @param item 订单项，不能为 {@code null}
     * @return 保留两位的实付单价；算不出来时返回 {@code null}
     */
    private BigDecimal realUnitPrice(OrderItemEntity item) {
        if (item.getRealAmount() == null || item.getSkuQuantity() == null || item.getSkuQuantity() <= 0) {
            return null;
        }
        return item.getRealAmount().divide(BigDecimal.valueOf(item.getSkuQuantity()), 2, RoundingMode.HALF_UP);
    }

    /**
     * 锁住订单行，返回锁住那一刻的行。
     *
     * <p>订单不存在时按 {@code ORDER_NOT_FOUND} 报：调用方在此之前已经判过归属，走到这里说明订单
     * 刚被硬删掉。</p>
     *
     * @param orderSn 订单号，不能为 {@code null}
     * @return 锁住的那一刻的订单行，不会为 {@code null}
     * @throws BaseException 订单不存在时抛 {@code ORDER_NOT_FOUND}
     */
    private OrderEntity lockOrderForAfterSale(String orderSn) {
        OrderEntity locked = this.baseMapper.lockOrderForAfterSale(orderSn);
        if (locked == null) {
            throw new BaseException(BaseCodeEnum.ORDER_NOT_FOUND);
        }
        return locked;
    }

    /**
     * 判断这张订单（或这一项）是不是已经有一笔进行中的售后。
     *
     * <p>"进行中"包括待处理、退货中与已完成三态；<b>已拒绝不算占用名额</b> —— 拒绝常因凭证不清楚
     * 或描述不全，会员补齐后可以再提交一次。</p>
     *
     * @param orderSn 订单号，不能为 {@code null}
     * @param type 售后类型，取值见 {@link AfterSaleTypeEnum}
     * @param skuId 退货时按 SKU 收窄；仅退款传 {@code null}，表示只看整单那条
     * @return 存在进行中的申请时为 {@code true}
     */
    private boolean hasActiveAfterSale(String orderSn, Integer type, Long skuId) {
        return orderReturnApplyService.count(new LambdaQueryWrapper<OrderReturnApplyEntity>()
                .eq(OrderReturnApplyEntity::getOrderSn, orderSn)
                .eq(OrderReturnApplyEntity::getType, type)
                .eq(skuId != null, OrderReturnApplyEntity::getSkuId, skuId)
                .in(OrderReturnApplyEntity::getStatus,
                        AfterSaleStatusEnum.PENDING.getCode(),
                        AfterSaleStatusEnum.RETURNING.getCode(),
                        AfterSaleStatusEnum.FINISHED.getCode())) > 0;
    }

    /** {@inheritDoc} */
    @Override
    public List<OrderReturnReasonEntity> listReturnReasons() {
        return orderReturnReasonService.list(new LambdaQueryWrapper<OrderReturnReasonEntity>()
                .eq(OrderReturnReasonEntity::getStatus, 1)
                .orderByAsc(OrderReturnReasonEntity::getSort)
                .orderByAsc(OrderReturnReasonEntity::getId));
    }

    /** {@inheritDoc} */
    @Override
    public List<OrderStatusOptionVo> listAfterSaleStatusOptions() {
        return Arrays.stream(AfterSaleStatusEnum.values())
                .map(value -> new OrderStatusOptionVo(value.getCode(), value.getMsg()))
                .collect(Collectors.toList());
    }

    /** {@inheritDoc} */
    @Override
    public PageVO<OrderReturnApplyEntity> queryAfterSalePage(AfterSalePageQuery query) {
        LambdaQueryWrapper<OrderReturnApplyEntity> wrapper = new LambdaQueryWrapper<OrderReturnApplyEntity>()
                .eq(StringUtils.hasText(query.getType()), OrderReturnApplyEntity::getType, query.getType())
                .eq(StringUtils.hasText(query.getStatus()), OrderReturnApplyEntity::getStatus, query.getStatus())
                .ge(StringUtils.hasText(query.getStartTime()),
                        OrderReturnApplyEntity::getCreateTime, query.getStartTime())
                .le(StringUtils.hasText(query.getEndTime()),
                        OrderReturnApplyEntity::getCreateTime, query.getEndTime())
                .and(StringUtils.hasText(query.getKey()), w -> w
                        .like(OrderReturnApplyEntity::getOrderSn, query.getKey())
                        .or().like(OrderReturnApplyEntity::getMemberUsername, query.getKey()))
                .orderByDesc(OrderReturnApplyEntity::getCreateTime)
                .orderByDesc(OrderReturnApplyEntity::getId);

        IPage<OrderReturnApplyEntity> page = orderReturnApplyService.page(query.toPage(), wrapper);
        List<OrderReturnApplyEntity> records = page.getRecords();
        fillAfterSaleTexts(records);

        return new PageVO<>(page.getTotal(), records);
    }

    /** {@inheritDoc} */
    @Override
    public OrderReturnApplyEntity getOrderReturnApply(Long id) {
        OrderReturnApplyEntity apply = orderReturnApplyService.getById(id);
        if (apply != null) {
            fillAfterSaleTexts(List.of(apply));
        }
        return apply;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditAfterSale(AfterSaleAuditVo vo, String operator) {
        OrderReturnApplyEntity apply = orderReturnApplyService.getById(vo.getId());
        if (apply == null) {
            throw new BaseException(BaseCodeEnum.ORDER_AFTER_SALE_NOT_FOUND);
        }

        OrderEntity order = getOrderByOrderSn(apply.getOrderSn());
        if (order == null) {
            // 硬删订单会连售后记录一起清掉，正常走不到这里；真走到说明有人手工删过库
            throw new BaseException(BaseCodeEnum.ORDER_NOT_FOUND);
        }

        if (!Boolean.TRUE.equals(vo.getApprove())) {
            rejectAfterSale(apply, order, vo.getHandleNote(), operator);
            return;
        }

        if (AfterSaleTypeEnum.isRefund(apply.getType())) {
            approveRefund(apply, order, vo.getHandleNote(), operator);
        } else {
            approveReturning(apply, order, vo.getHandleNote(), operator);
        }
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void receiveReturn(Long id, String receiveNote, String operator) {
        OrderReturnApplyEntity apply = orderReturnApplyService.getById(id);
        if (apply == null) {
            throw new BaseException(BaseCodeEnum.ORDER_AFTER_SALE_NOT_FOUND);
        }

        // 先向平台退款、再改本地：与整单退款同一条判据 —— 没拿到"钱确实退了"的应答就什么都不改
        MockRefundResultVo result = refundByPlatform(apply);

        if (orderReturnApplyDao.finishReturn(apply.getId(), operator, receiveNote) == 0) {
            // 不是"退货中"的单收不了货：重复点、或对没通过审核的单点，都会落在这里
            throw new BaseException(BaseCodeEnum.ORDER_AFTER_SALE_STATUS_INVALID);
        }

        OrderEntity order = getOrderByOrderSn(apply.getOrderSn());
        saveRefundInfo(apply, result, order == null ? null : order.getPayType());

        if (order != null) {
            // 订单状态不变（还是已完成），操作史记的是这次收货
            saveOperateHistory(order.getId(), order.getStatus(), operator,
                    "商家确认收到退货：" + apply.getSkuName());
        }
    }

    /**
     * 拒绝售后申请：两条链共用，只改申请单。
     *
     * <p>订单状态、库存、券与积分一个都不动。拒绝必须写理由，会员要靠它判断能不能补齐材料再申请。</p>
     *
     * @param apply 待拒绝的申请，不能为 {@code null}
     * @param order 申请所属订单，不能为 {@code null}
     * @param handleNote 驳回理由，不能为空
     * @param operator 操作人（管理员用户名）
     * @throws BaseException 没写理由 {@code ORDER_AFTER_SALE_NOTE_REQUIRED}、
     *         申请已被处理过 {@code ORDER_AFTER_SALE_STATUS_INVALID}
     */
    private void rejectAfterSale(OrderReturnApplyEntity apply, OrderEntity order, String handleNote, String operator) {
        if (!StringUtils.hasText(handleNote)) {
            throw new BaseException(BaseCodeEnum.ORDER_AFTER_SALE_NOTE_REQUIRED);
        }

        if (orderReturnApplyDao.rejectApply(apply.getId(), operator, handleNote) == 0) {
            // 已经通过或已经处理完的单不能再拒绝：那时货可能已经在路上，改成"已拒绝"会让会员钱货两空
            throw new BaseException(BaseCodeEnum.ORDER_AFTER_SALE_STATUS_INVALID);
        }

        saveOperateHistory(order.getId(), order.getStatus(), operator, "驳回售后申请：" + handleNote);
    }

    /**
     * 仅退款审核通过：先向平台退款，再作废订单并做善后。
     *
     * <p>顺序是刻意的：<b>先拿到"钱确实退了"的应答，才改本地状态</b>。反过来先作废订单，平台失败时
     * 会留下"订单已作废、钱没退"的账 —— 那是会员受损的方向；而这个顺序最坏只留下"平台退了、本地没记"，
     * 且重试是安全的（商户退款单号由申请单主键派生，平台侧认得出是同一笔）。</p>
     *
     * @param apply 待处理的退款申请，不能为 {@code null}
     * @param order 申请所属订单，不能为 {@code null}
     * @param handleNote 处理备注，可为 {@code null}
     * @param operator 操作人（管理员用户名）
     * @throws BaseException 订单状态不是已付款 {@code ORDER_STATUS_INVALID}、
     *         申请已被处理过 {@code ORDER_AFTER_SALE_STATUS_INVALID}、平台退款失败 {@code ORDER_REFUND_FAILED}
     */
    private void approveRefund(OrderReturnApplyEntity apply, OrderEntity order, String handleNote, String operator) {
        // 锁住订单行：从这一刻到事务提交，发货之类的操作改不动这一行。
        // 不锁的话，退款应答回来时订单可能已经变成"已发货"，下面那条 WHERE status = 1 的作废语句
        // 会改 0 行、整个事务回滚，留下"钱退了、账没有"
        OrderEntity locked = lockOrderForAfterSale(order.getOrderSn());
        if (!OrderStatusEnum.PAYED.getCode().equals(locked.getStatus())) {
            // 申请提交之后、审核之前被发货了：钱还没动，直接拒绝这次审核
            throw new BaseException(BaseCodeEnum.ORDER_STATUS_INVALID);
        }

        // ① 向平台退款。失败关闭：拿不到"钱确实退了"的应答就什么都不改
        MockRefundResultVo result = refundByPlatform(apply);

        // ② 申请单置为已完成：带 type = 0 与 status = 0 的条件更新，重复审核只有第一次生效
        if (orderReturnApplyDao.finishRefund(apply.getId(), operator, handleNote) == 0) {
            throw new BaseException(BaseCodeEnum.ORDER_AFTER_SALE_STATUS_INVALID);
        }

        // ③ 记退款流水：这一行是"钱确实退出去了"的凭证
        saveRefundInfo(apply, result, locked.getPayType());

        // ④ 订单作废并善后：解锁库存、退回已核销的券、退回抵扣的积分。
        //    与关单同一套动作，差别只在来源状态 —— 这条从"已付款(1)"来
        if (!refundCloseOrder(locked, operator)) {
            throw new BaseException(BaseCodeEnum.ORDER_STATUS_INVALID);
        }
    }

    /**
     * 退货审核通过：把申请置为"退货中"，并把商家的退货地址发给会员。
     *
     * <p>这一步不动钱：货还在会员手里，等他寄回来、商家确认收到才退款。</p>
     *
     * @param apply 待处理的退货申请，不能为 {@code null}
     * @param order 申请所属订单，不能为 {@code null}
     * @param handleNote 处理备注，可为 {@code null}
     * @param operator 操作人（管理员用户名）
     * @throws BaseException 申请已被处理过 {@code ORDER_AFTER_SALE_STATUS_INVALID}
     */
    private void approveReturning(OrderReturnApplyEntity apply, OrderEntity order, String handleNote, String operator) {
        int affected = orderReturnApplyDao.approveReturning(apply.getId(), operator,
                OrderConstant.RETURN_COMPANY_ADDRESS, OrderConstant.RETURN_RECEIVER,
                OrderConstant.RETURN_RECEIVER_PHONE, handleNote);
        if (affected == 0) {
            throw new BaseException(BaseCodeEnum.ORDER_AFTER_SALE_STATUS_INVALID);
        }

        saveOperateHistory(order.getId(), order.getStatus(), operator,
                "退货审核通过，已下发退货地址：" + apply.getSkuName());
    }

    /**
     * 整单退款成功后的订单作废与善后，与关单是同一套动作。
     *
     * <p>三件善后：通知仓库释放锁定中的库存、退回已核销的优惠券、退回抵扣的积分。
     * <b>不扣回支付时赠送的积分与成长值</b>：退款有后台审核这道闸门，而成长值"只增不减、
     * 等级只升不降"是既有决策，扣它会把那条决策打破。</p>
     *
     * <p>本方法自己不承担事务边界（私有方法上注解不生效），由调用方保证。</p>
     *
     * @param order 待作废的订单，其状态必须已被锁住且为已付款
     * @param operateMan 操作人（管理员用户名）
     * @return {@code true} 表示确实作废了；{@code false} 表示订单状态不是已付款，未做任何变更
     */
    private boolean refundCloseOrder(OrderEntity order, String operateMan) {
        if (this.baseMapper.refundCloseOrder(order.getOrderSn()) == 0) {
            log.warn("整单退款作废未生效，订单状态不是已付款：orderSn={}, status={}",
                    order.getOrderSn(), order.getStatus());
            return false;
        }

        saveOperateHistory(order.getId(), OrderStatusEnum.CANCLED.getCode(), operateMan, "整单退款成功，订单作废");

        OrderTo orderTo = new OrderTo();
        BeanUtils.copyProperties(order, orderTo);
        // copyProperties 拷到的是锁住那一刻的对象，status 仍是已付款；ware 按库存工作单解锁、
        // 不读这个字段，但 OrderTo 是订单快照，发出去的状态必须与库里一致
        orderTo.setStatus(OrderStatusEnum.CANCLED.getCode());

        // 不吞异常：消息记录写不进去就整体回滚，宁可不作废，也不能作废了却不释放库存
        reliableMqPublisher.publish(MqConstant.Exchanges.ORDER, MqConstant.RoutingKeys.ORDER_CLOSED, orderTo);

        if (order.getCouponId() != null) {
            refundCoupon(order.getOrderSn());
        }

        refundIntegration(order.getMemberId(), order.getOrderSn(), order.getUseIntegration());

        return true;
    }

    /**
     * 向模拟支付平台发起退款，返回平台的应答。
     *
     * <p>这是整条售后链路上唯一一次对外发起资金动作：<b>平台说资金动了才算退了</b>，
     * 只看"接口调用成功"会把"重复退款、钱没动"当成退款成功。</p>
     *
     * <p>商户退款单号由订单号与申请单主键拼成：一张申请单对应一次退款请求，天然唯一、可追溯，
     * 不必另立一套编号规则；它同时是平台侧的幂等锚 —— 重复提交同一个号，真实平台回的是同一笔结果。</p>
     *
     * @param apply 售后申请，不能为 {@code null}
     * @return 平台应答，{@code fund_change} 已确认为 {@code Y}
     * @throws BaseException 平台不可用、商户凭证不对（平台回 403）、平台没确认资金变动
     *         {@code ORDER_REFUND_FAILED}；订单查不到可用的支付交易号时也抛它
     */
    private MockRefundResultVo refundByPlatform(OrderReturnApplyEntity apply) {
        MockRefundRequestVo request = new MockRefundRequestVo();
        request.setOutRequestNo(apply.getOrderSn() + "-AS" + apply.getId());
        request.setRefundAmount(apply.getReturnAmount() == null
                ? "0.00"
                : apply.getReturnAmount().setScale(2, RoundingMode.HALF_UP).toPlainString());
        request.setTradeNo(requireTradeNo(apply.getOrderSn()));

        MockRefundResultVo result;
        try {
            result = mockPayFeignService.refund(mockMerchantSign, request);
        } catch (Exception e) {
            log.error("向模拟支付平台发起退款失败：orderSn={}, applyId={}", apply.getOrderSn(), apply.getId(), e);
            throw new BaseException(BaseCodeEnum.ORDER_REFUND_FAILED);
        }

        // fund_change = N 表示资金没有变动（典型场景是这笔已经退过）。走到这里说明申请单还在待处理，
        // 本来就不该出现"已经退过"的第二次请求，所以按未成功处理并留下平台原文
        if (result == null || !result.isFundChanged()) {
            log.error("平台未确认退款成功：orderSn={}, applyId={}, result={}",
                    apply.getOrderSn(), apply.getId(), JSON.toJSONString(result));
            throw new BaseException(BaseCodeEnum.ORDER_REFUND_FAILED);
        }
        return result;
    }

    /**
     * 取原支付交易号，退款请求要带上它让平台定位退哪笔钱。
     *
     * <p>取最后一条支付流水：一笔订单可能有多条回调流水（重复通知各写一条）。</p>
     *
     * @param orderSn 订单号，不能为 {@code null}
     * @return 支付交易号，不会为 {@code null}
     * @throws BaseException 订单没有可用的交易号时抛 {@code ORDER_REFUND_FAILED} ——
     *         已付款的订单正常都有一条带交易号的流水，取不到说明账本不全，不能拿着空交易号去退款
     */
    private String requireTradeNo(String orderSn) {
        List<PaymentInfoEntity> payments = listPaymentInfo(orderSn);
        String tradeNo = payments.isEmpty() ? null : payments.get(payments.size() - 1).getAlipayTradeNo();
        if (tradeNo == null || tradeNo.isBlank()) {
            log.error("订单没有可用的支付交易号，无法发起退款：orderSn={}", orderSn);
            throw new BaseException(BaseCodeEnum.ORDER_REFUND_FAILED);
        }
        return tradeNo;
    }

    /**
     * 写一行退款流水。
     *
     * <p>它是资金台账而不是申请表：申请人、申请时间与处理理由都在申请单上，这里只记钱。</p>
     *
     * @param apply 售后申请，不能为 {@code null}
     * @param result 平台应答，不能为 {@code null}
     * @param payType 订单当初的支付方式，作为退款渠道（退款是原路退回，渠道不该重新选）；可为 {@code null}
     */
    private void saveRefundInfo(OrderReturnApplyEntity apply, MockRefundResultVo result, Integer payType) {
        RefundInfoEntity refund = new RefundInfoEntity();
        // 两条链都填申请单 ID：这一列回答"这笔退款由哪张售后单产生"，仅退款也有申请单。
        // 留 null 的话这条流水关联不回任何订单 —— 后台看不到它、硬删订单时也清不掉它
        refund.setOrderReturnId(apply.getId());
        refund.setRefund(apply.getReturnAmount());
        refund.setRefundSn(result.getRefundId());
        refund.setRefundStatus(OrderConstant.REFUND_STATUS_SUCCESS);
        refund.setRefundChannel(payType);
        // 平台原文照存：这是对账与排查时唯一能证明"平台当时怎么回的"的东西
        refund.setRefundContent(JSON.toJSONString(result));
        refundInfoService.save(refund);
    }

    /**
     * 退回整单退款时那张已核销的券。
     *
     * <p>与关单用的 {@code unlock} 不是同一个接口：关单退的是"占用中"的券，这里退的是"已使用"的券，
     * 后者还要把券模板的已用计数减回去。不吞异常：券没退回去就是会员白丢一张券。</p>
     *
     * @param orderSn 订单号，不能为 {@code null}
     * @throws BaseException 券服务不可用时抛出
     */
    private void refundCoupon(String orderSn) {
        R<Void> response;
        try {
            response = couponFeignService.refundCoupon(orderSn);
        } catch (Exception e) {
            log.error("退款时退回优惠券失败，orderSn={}", orderSn, e);
            throw new BaseException(BaseCodeEnum.ORDER_COUPON_UNAVAILABLE);
        }
        if (response == null || response.getCode() != 0) {
            throw new BaseException(
                    response == null ? BaseCodeEnum.ORDER_COUPON_UNAVAILABLE.getCode() : response.getCode(),
                    response == null ? BaseCodeEnum.ORDER_COUPON_UNAVAILABLE.getMsg() : response.getMsg());
        }
    }

    /**
     * 给售后申请填上类型与状态的文案。
     *
     * @param records 待填充的申请列表，可为空
     */
    private void fillAfterSaleTexts(List<OrderReturnApplyEntity> records) {
        for (OrderReturnApplyEntity record : records) {
            record.setTypeText(AfterSaleTypeEnum.textOf(record.getType()));
            record.setStatusText(AfterSaleStatusEnum.textOf(record.getStatus()));
        }
    }

    /**
     * 给订单挂上售后申请，并算出两个"入口显不显示"的布尔值。
     *
     * <p>判据留在服务端：会员端只按 {@code canApplyRefund} / {@code canApplyReturn} 决定按钮显不显示，
     * 不自己拼"状态对不对、有没有申请过"这两个条件 —— 前端拼的规则迟早会与后端的闸门分叉
     * （与券列表每行回填 {@code allowedActions} 同源）。</p>
     *
     * <p>一次批量查完该页所有订单的申请再分组，不逐单查询。</p>
     *
     * @param orders 订单列表，可为空；调用方需保证订单项已装配
     */
    private void attachAfterSales(List<OrderEntity> orders) {
        if (orders == null || orders.isEmpty()) {
            return;
        }

        List<String> orderSns = orders.stream().map(OrderEntity::getOrderSn).collect(Collectors.toList());
        Map<String, List<OrderReturnApplyEntity>> byOrderSn = orderReturnApplyService.list(
                        new LambdaQueryWrapper<OrderReturnApplyEntity>()
                                .in(OrderReturnApplyEntity::getOrderSn, orderSns)
                                .orderByAsc(OrderReturnApplyEntity::getCreateTime)
                                .orderByAsc(OrderReturnApplyEntity::getId))
                .stream()
                .collect(Collectors.groupingBy(OrderReturnApplyEntity::getOrderSn));

        for (OrderEntity order : orders) {
            List<OrderReturnApplyEntity> applies = byOrderSn.getOrDefault(order.getOrderSn(), List.of());
            order.setAfterSaleList(applies.stream().map(this::toMemberAfterSaleVo).collect(Collectors.toList()));

            // 退款是整单的：只要有一笔进行中的退款申请，入口就不再出现
            boolean refundTaken = applies.stream().anyMatch(apply ->
                    AfterSaleTypeEnum.isRefund(apply.getType()) && isActiveAfterSale(apply));
            order.setCanApplyRefund(OrderStatusEnum.PAYED.getCode().equals(order.getStatus()) && !refundTaken);

            // 退货是按项的：把已有进行中退货申请的 SKU 收成一个集合，逐项判
            Set<Long> returnedSkuIds = applies.stream()
                    .filter(apply -> !AfterSaleTypeEnum.isRefund(apply.getType()) && isActiveAfterSale(apply))
                    .map(OrderReturnApplyEntity::getSkuId)
                    .collect(Collectors.toSet());
            boolean returnable = OrderStatusEnum.RECIEVED.getCode().equals(order.getStatus());
            if (order.getOrderItemEntityList() != null) {
                order.getOrderItemEntityList().forEach(item ->
                        item.setCanApplyReturn(returnable && !returnedSkuIds.contains(item.getSkuId())));
            }
        }
    }

    /**
     * 判断一条申请是不是"占着名额"：待处理、退货中、已完成三态都算；已拒绝不算。
     *
     * @param apply 申请，不能为 {@code null}
     * @return 占着名额时为 {@code true}
     */
    private boolean isActiveAfterSale(OrderReturnApplyEntity apply) {
        Integer status = apply.getStatus();
        return AfterSaleStatusEnum.PENDING.getCode().equals(status)
                || AfterSaleStatusEnum.RETURNING.getCode().equals(status)
                || AfterSaleStatusEnum.FINISHED.getCode().equals(status);
    }

    /**
     * 把申请单转成会员端看的投影。
     *
     * <p>刻意不直接把实体给会员端：实体上有 {@code handleMan}（处理这条申请的管理员用户名），
     * 那是内部人员信息；而且直接回实体意味着以后给表加任何一列都会自动出现在会员端。</p>
     *
     * @param apply 申请，不能为 {@code null}
     * @return 会员端投影，含类型与状态文案
     */
    private MemberAfterSaleVo toMemberAfterSaleVo(OrderReturnApplyEntity apply) {
        MemberAfterSaleVo vo = new MemberAfterSaleVo();
        vo.setId(apply.getId());
        vo.setOrderSn(apply.getOrderSn());
        vo.setType(apply.getType());
        vo.setTypeText(AfterSaleTypeEnum.textOf(apply.getType()));
        vo.setStatus(apply.getStatus());
        vo.setStatusText(AfterSaleStatusEnum.textOf(apply.getStatus()));
        vo.setReturnAmount(apply.getReturnAmount());
        vo.setSkuId(apply.getSkuId());
        vo.setSkuName(apply.getSkuName());
        vo.setSkuImg(apply.getSkuImg());
        vo.setSkuAttrsVals(apply.getSkuAttrsVals());
        vo.setSkuCount(apply.getSkuCount());
        vo.setReason(apply.getReason());
        vo.setDescription(apply.getDescription());
        vo.setDescPics(apply.getDescPics());
        vo.setCreateTime(apply.getCreateTime());
        vo.setHandleTime(apply.getHandleTime());
        vo.setHandleNote(apply.getHandleNote());
        vo.setCompanyAddress(apply.getCompanyAddress());
        vo.setReceiveMan(apply.getReceiveMan());
        vo.setReceivePhone(apply.getReceivePhone());
        vo.setReceiveTime(apply.getReceiveTime());
        vo.setReceiveNote(apply.getReceiveNote());
        return vo;
    }

    /* ═══════════════════ 订单创建（内部） ═══════════════════ */

    private void saveOrder(OrderCreateTo orderCreateTo) {

        OrderEntity order = orderCreateTo.getOrder();
        order.setModifyTime(new Date());
        order.setCreateTime(new Date());
        this.baseMapper.insert(order);
        // 订单已落库，自增主键此时才回填，操作史要用到它
        saveOperateHistory(order.getId(), order.getStatus(), order.getMemberUsername(), "提交订单");

        List<OrderItemEntity> orderItems = orderCreateTo.getOrderItems();
        // 订单项要记 order_id，而自增主键要等订单落库之后才回填，所以在这里补
        orderItems.forEach(orderItem -> orderItem.setOrderId(order.getId()));
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

        // 3. 按与确认页同一套逻辑重算运费与促销，两次算出的必须是同一个数
        FareVo fare = calcFare(address, cartItems, isFreeFreight(user.getId()));
        BigDecimal freightAmount = fare.getFreightAmount();

        // 4. 组装订单主表与订单项
        OrderEntity orderEntity = builderOrder(user, orderSn, submitVo, address, freightAmount);
        List<OrderItemEntity> orderItemEntities = builderOrderItems(orderSn, cartItems);

        // 5. 促销：减免额写进订单项；券的门槛与积分的封顶都要先扣掉它
        writePromotionToOrderItems(orderItemEntities, fare.getPromotionItems());

        // 6. 券：抵扣额由券服务按同一份购物车重算，再按它声明的范围摊到订单项上。
        //    必须在 computePrice 之前：订单优惠金额由各订单项汇总
        List<CouponCartItemVo> couponCartItems = toCouponCartItems(cartItems, fare.getPromotionItems());
        CouponUsableVo coupon = submitVo.getCouponHistoryId() == null
                ? null
                : requireCouponDiscount(user.getId(), couponCartItems, submitVo.getCouponHistoryId());
        writeCouponToOrderItems(orderItemEntities, splitCouponDiscount(couponCartItems, coupon));

        // 7. 积分：抵扣上限按"商品总额减促销减免再减券抵扣"的一半算，所以必须排在促销与券之后。
        //    不用积分的订单不调会员服务，免得被它的可用性拖住
        Integer requestedPoints = submitVo.getUseIntegration();
        int usePoints = 0;
        List<IntegrationDiscountItemVo> integrationShares = List.of();
        if (requestedPoints != null && requestedPoints > 0) {
            int balance = requireIntegrationBalance(user.getId());
            int limit = IntegrationConstant.maxUsablePoints(
                    sumItemAmount(cartItems).subtract(fare.getPromotionAmount()),
                    coupon == null ? BigDecimal.ZERO : coupon.getDiscountAmount(), balance);
            usePoints = resolveIntegrationPoints(limit, balance, requestedPoints);
            integrationShares = splitIntegration(IntegrationConstant.pointsToAmount(usePoints), cartItems,
                    promotionBySkuId(fare.getPromotionItems()));
        }
        writeIntegrationToOrderItems(orderItemEntities, integrationShares);
        // 不使用积分时写 0 而不是留 NULL：这一列的语义是"下单时使用的积分"，事实就是 0
        orderEntity.setUseIntegration(usePoints);

        // 8. 计算价格与积分
        computePrice(orderEntity, orderItemEntities);
        // 券模板 ID 落到订单上供展示与排查；核销与退回按订单号定位，不依赖这个字段
        orderEntity.setCouponId(coupon == null ? null : coupon.getCouponId());

        createTo.setOrder(orderEntity);
        createTo.setOrderItems(orderItemEntities);

        return createTo;
    }


    /**
     * 计算订单项的原价，即单价 × 数量。
     *
     * <p>组装订单项与重算实付金额都要用它，抽出来是为了两处不会算出两个数。
     *
     * @param orderItem 订单项，其单价与数量不能为 {@code null}
     * @return 未减任何优惠的金额
     */
    private BigDecimal originAmount(OrderItemEntity orderItem) {
        return orderItem.getSkuPrice().multiply(new BigDecimal(orderItem.getSkuQuantity().toString()));
    }


    /**
     * 汇总订单金额与积分。
     *
     * <p>订单头的各类优惠金额都是<b>各订单项对应字段的汇总</b>，不是独立算出来的：
     * 优惠券抵扣额必须先由 {@code writeCouponToOrderItems} 写到订单项上，这里才能汇总出非零值。
     *
     * @param orderEntity 待落库的订单主表，{@code freightAmount} 必须已有值
     * @param orderItemEntities 待落库的订单项，各类优惠金额必须已经填好
     */
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
        orderEntity.setAutoConfirmDay(OrderConstant.AUTO_CONFIRM_DAY);
        orderEntity.setConfirmStatus(0);
        // 支付方式在提交订单时已选定，落库记录下来；支付回调会按实际结果覆盖一次
        orderEntity.setPayType(submitVo.getPayType());
        orderEntity.setSourceType(OrderConstant.SOURCE_TYPE_PC);
        return orderEntity;
    }

    /**
     * 把订单上的收货地址快照拼成一行配送地址。
     *
     * <p>逐段跳过 null 与空串：省市区在库里允许为 null，不过滤会拼出"湖南 长沙 null 望城区"
     * 这种脏字符串。分隔符与会员端 {@code tools/address.ts} 的 {@code joinAddress} 一致。
     *
     * @param order 订单，取它的省市区与详细地址四段
     * @return 一行配送地址，段间以空格分隔；四段都为空时返回空串
     */
    private String joinDeliveryAddress(OrderEntity order) {
        return Arrays.asList(order.getReceiverProvince(), order.getReceiverCity(),
                        order.getReceiverRegion(), order.getReceiverDetailAddress())
                .stream()
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(" "));
    }

    /**
     * 把订单的支付方式换算成库存工作单口径。
     *
     * <p>两者取值口径不同：{@code oms_order.pay_type} 是 1 支付宝 / 2 微信 / 3 银联 / 4 货到付款，
     * 而工作单的 {@code payment_way} 只分 1 在线付款 / 2 货到付款，不能直拷。
     *
     * @param payType 下单时选择的支付方式，见 {@link PayConstant}
     * @return 货到付款返回 2，其余返回 1；本项目尚无货到付款链路，当前恒为 1
     */
    private Integer toPaymentWay(Integer payType) {
        return PayConstant.CASH_ON_DELIVERY.equals(payType) ? 2 : 1;
    }

    /**
     * 构建订单包含的全部订单项，并填好每项的赠送积分与成长值。
     *
     * <p>赠送量统一在 {@link #fillGift} 里填，不在 {@link #builderOrderItem} 里填：
     * 它要按 SPU 批量取一次积分设置，而单个订单项那个方法只看得到自己这一项。
     *
     * @param orderSn 订单号，写入每个订单项
     * @param cartItems 购物车中已勾选的购物项，不能为 {@code null}
     * @return 订单项列表；购物车没有已勾选商品时返回空列表
     */
    private List<OrderItemEntity> builderOrderItems(String orderSn, List<OrderItemVo> cartItems) {
        List<OrderItemEntity> orderItemEntities = cartItems.stream().map(cartItem -> {
            OrderItemEntity orderItemEntity = builderOrderItem(cartItem);
            orderItemEntity.setOrderSn(orderSn);
            return orderItemEntity;
        }).collect(Collectors.toList());

        // 一张单的多个商品共用一次远程调用，而不是每项各查一次
        fillGift(orderItemEntities);

        return orderItemEntities;
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
        orderItemEntity.setSpuPic(spuInfoData.getSpuPic());
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

        // 4. 商品的积分信息由 fillGift 在整张单的订单项都建好后统一填：
        //    赠送量要按 spuId 批量取积分设置，放在这里会变成每个订单项各查一次

        // 5. 订单项的价格信息
        orderItemEntity.setPromotionAmount(BigDecimal.ZERO);
        orderItemEntity.setCouponAmount(BigDecimal.ZERO);
        orderItemEntity.setIntegrationAmount(BigDecimal.ZERO);

        // 实付金额 = 原价（单价 × 数量）减去各类优惠
        orderItemEntity.setRealAmount(originAmount(orderItemEntity)
                .subtract(orderItemEntity.getCouponAmount())
                .subtract(orderItemEntity.getPromotionAmount())
                .subtract(orderItemEntity.getIntegrationAmount()));

        return orderItemEntity;
    }

    /**
     * 填充每个订单项的赠送购物积分与成长值。
     *
     * <p>算法在 {@link GiftCalculator} 里，与商品详情页展示"买它可得多少"是同一份 ——
     * 各写一份必然分叉，页面显示的数与下单实际得到的数一旦对不上，会员看到的就是"说好的没给"。
     *
     * <p>算好写进订单项即成快照，付款时 member 侧按这份快照入账，不在付款时重算：
     * 之后改配置、改倍率都不会动到已经下过的单。
     *
     * @param orderItems 已建好的订单项，{@code skuPrice} 与 {@code skuQuantity} 必须已填
     */
    private void fillGift(List<OrderItemEntity> orderItems) {
        if (orderItems.isEmpty()) {
            return;
        }

        List<Long> skuIds = orderItems.stream()
                .map(OrderItemEntity::getSkuId)
                .filter(Objects::nonNull)
                .toList();
        Map<Long, SkuBoundsVo> boundsMap = loadSkuBounds(skuIds);

        for (OrderItemEntity orderItem : orderItems) {
            BigDecimal originAmount = orderItem.getSkuPrice()
                    .multiply(BigDecimal.valueOf(orderItem.getSkuQuantity()));
            GiftCalculator.Amount gift = GiftCalculator.of(originAmount, orderItem.getSkuQuantity(),
                    boundsMap.get(orderItem.getSkuId()));

            orderItem.setGiftIntegration(gift.integration());
            orderItem.setGiftGrowth(gift.growth());
        }
    }

    /**
     * 按 skuId 批量取积分设置。
     *
     * <p><b>失败开放</b>：赠送是下单的附带动作，取不到配置时按"没配置"处理并记 warn，
     * 只送基础那一份。判据与券列表同源 —— 它不并进金额、不决定这笔单能不能下，
     * 让它挡住整笔下单，代价远大于少送一份配置量。
     *
     * @param skuIds SKU 标识集合，可以为空集合
     * @return skuId 到积分设置的映射；取不到或没有配置过的 SKU 不在映射里，不返回 {@code null}
     */
    private Map<Long, SkuBoundsVo> loadSkuBounds(List<Long> skuIds) {
        if (skuIds.isEmpty()) {
            return Map.of();
        }

        R<List<SkuBoundsVo>> r;
        try {
            r = couponFeignService.skuBounds(skuIds);
        } catch (Exception e) {
            log.warn("取商品积分设置失败，本单按未配置处理: {}", e.getMessage());
            return Map.of();
        }

        if (r == null || r.getCode() != 0 || r.getData() == null) {
            log.warn("取商品积分设置失败，本单按未配置处理: {}", r == null ? "响应为空" : r.getMsg());
            return Map.of();
        }

        return r.getData().stream()
                .filter(bounds -> bounds.getSkuId() != null)
                .collect(Collectors.toMap(SkuBoundsVo::getSkuId, bounds -> bounds, (first, second) -> second));
    }

    /**
     * 算本单可得的赠送积分与成长值，供确认页与换地址后的重算展示。
     *
     * <p>算法与下单入账的是同一份（{@link GiftCalculator}），所以页面显示的数就是付款后到账的数；
     * 区别只在于下单时它被写进订单项快照，这里只回给前端显示。
     *
     * <p>失败开放，判据见 {@link #loadSkuBounds}。
     *
     * @param cartItems 已勾选的购物项，不能为 {@code null}
     * @return 本单可得的购物积分与成长值；购物车为空时两份都是 0
     */
    private GiftCalculator.Amount totalGift(List<OrderItemVo> cartItems) {
        List<Long> skuIds = cartItems.stream()
                .map(OrderItemVo::getSkuId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, SkuBoundsVo> boundsMap = loadSkuBounds(skuIds);

        int integration = 0;
        int growth = 0;
        for (OrderItemVo cartItem : cartItems) {
            if (cartItem.getPrice() == null || cartItem.getCount() == null) {
                continue;
            }
            GiftCalculator.Amount gift = GiftCalculator.of(itemAmount(cartItem), cartItem.getCount(),
                    boundsMap.get(cartItem.getSkuId()));
            integration += gift.integration();
            growth += gift.growth();
        }
        return new GiftCalculator.Amount(integration, growth);
    }

    private record GiftAmount(int integration, int growth) {
    }


    /**
     * {@inheritDoc}
     *
     * <p>流水与订单状态在同一事务内写入，支付成功时两者必须一起生效。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public String handlePayResult(PayAsyncVo asyncVo) {

        String orderSn = asyncVo.getOut_trade_no();
        String tradeStatus = asyncVo.getTrade_status();

        // 只有这两个状态代表付款成功，其余状态只落流水，不动订单
        if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
            savePaymentInfo(asyncVo);
            return "success";
        }

        OrderEntity order = this.getOrderByOrderSn(orderSn);
        if (order == null) {
            // 订单查不到时不做任何变更，但流水照落：这笔钱到过账的事实要留下
            log.warn("支付回调的订单不存在，跳过核销与状态更新：{}", orderSn);
            savePaymentInfo(asyncVo);
            return "success";
        }

        // 钱已到账但订单已关单：留流水并告警，但不改状态 —— 库存已释放
        if (OrderStatusEnum.CANCLED.getCode().equals(order.getStatus())) {
            log.error("已取消的订单收到支付成功回调，订单状态未变更，需人工核对退款：orderSn={}, tradeNo={}",
                    orderSn, asyncVo.getTrade_no());
            savePaymentInfo(asyncVo);
            return "success";
        }

        // 已付款及之后说明是重复投递：不落流水、不核销，直接应答停止重投
        if (!OrderStatusEnum.CREATE_NEW.getCode().equals(order.getStatus())) {
            log.info("支付回调重复投递，订单已处理过：orderSn={}, status={}", orderSn, order.getStatus());
            return "success";
        }

        savePaymentInfo(asyncVo);

        // 没用券的订单不调券服务：那边把"找不到券"当成错误，无条件调用会让无券订单支付失败
        if (order.getCouponId() != null) {
            // 核销放在改状态之前：核销失败时整个事务回滚（含上面那条流水），
            // 支付宝重投能重新走一遍；先改状态的话重投会落到已支付分支，核销就补不上了
            consumeCoupon(orderSn);
        }

        // 赠送积分与核销券同一个位置、同一个理由。重投不会重复赠送：
        // member 侧按订单号与变动类型做了幂等
        grantIntegration(order.getMemberId(), orderSn, order.getIntegration());

        // 赠送成长值：口径与积分一致，同样排在改状态之前
        grantGrowth(order.getMemberId(), orderSn, order.getGrowth());

        int affected = this.updateOrderStatus(orderSn, OrderStatusEnum.PAYED.getCode(), PayConstant.ALIPAY,
                "支付宝支付成功");
        if (affected == 0) {
            // 查询与更新之间订单被并发改走，条件更新拦住了覆盖
            log.error("支付回调的状态更新未生效，订单状态已被并发变更：orderSn={}", orderSn);
        }

        return "success";
    }


    /**
     * 落一条支付宝支付流水。
     *
     * <p>从支付宝通知里取出订单号、交易号、金额与状态，交给
     * {@link #savePaymentInfo(PaymentInfoEntity)} 落库。
     *
     * @param asyncVo 通知参数，不能为 {@code null}
     */
    private void savePaymentInfo(PayAsyncVo asyncVo) {

        PaymentInfoEntity paymentInfo = new PaymentInfoEntity();
        paymentInfo.setOrderSn(asyncVo.getOut_trade_no());
        paymentInfo.setAlipayTradeNo(asyncVo.getTrade_no());
        paymentInfo.setTotalAmount(parseAmount(asyncVo.getBuyer_pay_amount()));
        paymentInfo.setSubject(asyncVo.getBody());
        paymentInfo.setPaymentStatus(asyncVo.getTrade_status());
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setCallbackTime(asyncVo.getNotify_time());
        savePaymentInfo(paymentInfo);
    }


    /**
     * 落一条微信支付流水。
     *
     * <p>{@code PayResponse} 的 {@code outTradeNo} 装的是微信交易号 {@code transaction_id}，
     * {@code orderId} 装的是本系统的订单号。微信通知里没有交易内容与状态原文，因此
     * {@code subject} 留空、{@code payment_status} 记渠道侧的约定值。
     *
     * @param orderEntity 已查出的订单，不能为 {@code null}
     * @param payResponse 微信通知的解析结果，不能为 {@code null}
     */
    private void saveWxPaymentInfo(OrderEntity orderEntity, PayResponse payResponse) {

        PaymentInfoEntity paymentInfo = new PaymentInfoEntity();
        paymentInfo.setOrderSn(orderEntity.getOrderSn());
        paymentInfo.setAlipayTradeNo(payResponse.getOutTradeNo());
        // 金额是 Double，valueOf 走 toString，无二进制误差
        paymentInfo.setTotalAmount(payResponse.getOrderAmount() == null
                ? null : BigDecimal.valueOf(payResponse.getOrderAmount()));
        paymentInfo.setPaymentStatus("WXPAY_SUCCESS");
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setCallbackTime(new Date());
        savePaymentInfo(paymentInfo);
    }


    /**
     * 落一条支付流水，同一个平台交易号只落一条。
     *
     * <p>支付平台会对同一笔交易多次通知（如退款后再发一条 {@code TRADE_CLOSED}），而
     * {@code oms_payment_info} 的 {@code alipay_trade_no} 是唯一键，重复插入会让整个事务回滚、
     * 回调方收到失败应答后继续重投，因此已存在时只记日志、不覆盖已落库的内容。
     *
     * @param paymentInfo 待落库的流水，不能为 {@code null}；其 {@code alipayTradeNo} 是查重键，
     *                    也不能为 {@code null}
     */
    private void savePaymentInfo(PaymentInfoEntity paymentInfo) {

        String tradeNo = paymentInfo.getAlipayTradeNo();
        List<PaymentInfoEntity> recorded = this.paymentInfoService.list(
                new LambdaQueryWrapper<PaymentInfoEntity>()
                        .eq(PaymentInfoEntity::getAlipayTradeNo, tradeNo));

        if (!recorded.isEmpty()) {
            String recordedStatus = recorded.get(0).getPaymentStatus();
            if (!Objects.equals(recordedStatus, paymentInfo.getPaymentStatus())) {
                log.warn("同一交易号的支付状态已变化，不覆盖已落库的流水：tradeNo={}, 已记录={}, 本次={}",
                        tradeNo, recordedStatus, paymentInfo.getPaymentStatus());
            }
            return;
        }

        this.paymentInfoService.save(paymentInfo);
    }


    /**
     * 把支付宝通知里的金额字符串转成金额。
     *
     * <p>通知字段都是字符串，且并非每条通知都带 {@code buyer_pay_amount}（如 {@code TRADE_CLOSED}）。
     * 解析失败时返回 {@code null}，让这条通知的其余字段照常入库。
     *
     * @param amount 金额字符串，可为 {@code null} 或空白
     * @return 解析出的金额；{@code amount} 为空或不是合法数字时返回 {@code null}
     */
    private BigDecimal parseAmount(String amount) {

        if (!StringUtils.hasText(amount)) {
            return null;
        }
        try {
            return new BigDecimal(amount);
        } catch (NumberFormatException e) {
            // 金额缺失不该让整条回调回滚：平台收到失败应答会一直重投
            log.warn("支付回调的金额无法解析，按空值记录：amount={}", amount);
            return null;
        }
    }


    /**
     * 更新订单的支付状态与支付方式，并写一条支付操作史。
     *
     * <p>操作史只在状态确实被推进时写：重复回调只会更新 0 行，不留记录。</p>
     *
     * @param orderSn 订单号
     * @param code 目标状态码，取自 {@link OrderStatusEnum}
     * @param payType 支付方式，取自 {@code PayConstant}
     * @param note 写进操作史的说明，按支付渠道区分
     * @return 受影响行数；1 表示更新生效，0 表示订单不存在或已不是待付款状态
     */
    private int updateOrderStatus(String orderSn, Integer code, Integer payType, String note) {
        int affected = this.baseMapper.updateOrderStatus(orderSn, code, payType);
        if (affected > 0) {
            // 只有真的推进了状态才留痕：重复回调改到 0 行，不留记录
            OrderEntity paid = getOrderByOrderSn(orderSn);
            if (paid != null) {
                saveOperateHistory(paid.getId(), code, "系统", note);
            }
        }
        return affected;
    }

    /** {@inheritDoc} */
    @Transactional(rollbackFor = Exception.class)
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

        // 幂等：已支付或已取消时直接应答成功，不重复推进；重投不会改变结果
        Integer status = orderEntity.getStatus();
        if (status.equals(OrderStatusEnum.PAYED.getCode()) || status.equals(OrderStatusEnum.CANCLED.getCode())) {
            log.info("微信支付回调重复投递，订单已处理过：orderSn={}, status={}", orderEntity.getOrderSn(), status);
            return WX_SUCCESS_RESPONSE;
        }

        // TODO: 微信异步通知的金额未与订单应付金额比对，需补校验并在不一致时告警

        // 3. 落交易流水
        saveWxPaymentInfo(orderEntity, payResponse);

        // 4. 核销优惠券。核销失败时整个事务回滚，重投能重新走一遍；
        //    没用券的订单不调券服务，那边把"找不到券"当成错误
        String orderSn = orderEntity.getOrderSn();
        if (orderEntity.getCouponId() != null) {
            consumeCoupon(orderSn);
        }

        // 赠送积分：与核销券同一位置、同一理由，失败时整个事务回滚，重投能补上
        grantIntegration(orderEntity.getMemberId(), orderSn, orderEntity.getIntegration());

        // 赠送成长值：与积分同一位置、同一理由
        grantGrowth(orderEntity.getMemberId(), orderSn, orderEntity.getGrowth());

        // 5. 修改订单支付状态
        this.updateOrderStatus(orderSn, OrderStatusEnum.PAYED.getCode(), PayConstant.WXPAY, "微信支付成功");

        // 6. 应答 SUCCESS，微信收到后即停止重发通知
        return WX_SUCCESS_RESPONSE;
    }

    /**
     * {@inheritDoc}
     *
     * <p>流水、券核销与订单状态在同一事务内写入，三者必须一起生效。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void handleMockPayResult(String orderSn, String ticket) {

        // 1. 验票。比较与删除必须是原子的，否则两个并发提交会同时通过；返回 1 表示消费成功
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        Long consumed = redisTemplate.execute(
                new DefaultRedisScript<>(script, Long.class),
                Arrays.asList(OrderConstant.MOCK_PAY_TICKET_PREFIX + orderSn),
                ticket
        );
        if (consumed == null || consumed == 0L) {
            log.info("测试支付票据验证失败，orderSn={}", orderSn);
            throw new BaseException(BaseCodeEnum.ORDER_TOKEN_INVALID);
        }

        // 2. 复核状态。票据有效期内订单仍可能被超时关单，关闭后不能再收款
        OrderEntity order = getOrderByOrderSn(orderSn);
        if (order == null || !OrderStatusEnum.CREATE_NEW.getCode().equals(order.getStatus())) {
            log.info("测试支付时订单状态已变更，orderSn={}", orderSn);
            throw new BaseException(BaseCodeEnum.ORDER_STATUS_INVALID);
        }

        // 3. 落交易流水。交易号前缀与支付状态都带 MOCK，与真实渠道回填的取值不会重名
        PaymentInfoEntity paymentInfo = new PaymentInfoEntity();
        paymentInfo.setOrderSn(orderSn);
        paymentInfo.setOrderId(order.getId());
        paymentInfo.setAlipayTradeNo("MOCK-" + System.currentTimeMillis());
        paymentInfo.setTotalAmount(order.getPayAmount());
        paymentInfo.setSubject("测试支付");
        paymentInfo.setPaymentStatus("MOCK_SUCCESS");
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setCallbackTime(new Date());
        paymentInfoService.save(paymentInfo);

        // 4. 核销券排在改状态之前：核销失败时整个事务回滚（含上面那条流水），重试还能把券补上
        if (order.getCouponId() != null) {
            consumeCoupon(orderSn);
        }

        // 5. 赠送积分，同样排在改状态之前
        grantIntegration(order.getMemberId(), orderSn, order.getIntegration());

        // 赠送成长值，与积分同一位置
        grantGrowth(order.getMemberId(), orderSn, order.getGrowth());

        // 6. 置为已付款
        updateOrderStatus(orderSn, OrderStatusEnum.PAYED.getCode(), PayConstant.MOCK, "测试支付成功");
    }


    /** {@inheritDoc} */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deliverOrder(DeliverVo vo, String operator) {

        // 这个编码会写进订单并被会员看到，不能由调用方随便给：不在枚举内就是参数问题，不是状态问题
        if (!DeliveryCompanyEnum.isValid(vo.getDeliveryCompany())) {
            throw new ValidationException("deliveryCompany", "不是支持的物流公司");
        }

        // 真实场景下单号来自承运商出库接口，项目未对接，故由本系统按订单号同一个生成器给出
        String deliverySn = vo.getDeliveryCompany() + IdWorker.getTimeId();

        // 带状态条件的 UPDATE：判断"是不是已付款"与写入"置为已发货"是同一条 SQL。
        // 并发点两次发货、或对已发货/已取消的单再点一次，都只有第一个能成功
        int affected = this.baseMapper.deliverOrder(vo.getOrderSn(), vo.getDeliveryCompany(), deliverySn);
        if (affected == 0) {
            log.info("发货失败，订单不存在或不是已付款状态：orderSn={}", vo.getOrderSn());
            throw new BaseException(BaseCodeEnum.ORDER_STATUS_INVALID);
        }

        // 操作史要订单 ID：这里只有订单号，补一次查询
        OrderEntity delivered = getOrderByOrderSn(vo.getOrderSn());
        if (delivered != null) {
            saveOperateHistory(delivered.getId(), OrderStatusEnum.SENDED.getCode(), operator,
                    "发货：" + vo.getDeliveryCompany() + " " + deliverySn);
        }

        // 出库实扣交给 ware 异步做：消息记录与上面的状态变更同一事务落库，提交后才投出去。
        // 不吞异常 —— 消息记录写不进去就整体回滚，宁可不发货，也不能发了货却不扣库存
        OrderDeliveredTo deliveredTo = new OrderDeliveredTo(vo.getOrderSn());
        reliableMqPublisher.publish(MqConstant.Exchanges.ORDER, MqConstant.RoutingKeys.ORDER_DELIVERED, deliveredTo);
    }

    /** {@inheritDoc} */
    @Override
    public void confirmReceive(MemberResponseVo user, String orderSn) {
        OrderEntity order = requireOwnOrder(user, orderSn);

        // 带状态条件的 UPDATE：判断"是不是已发货"与写入"置为已完成"是同一条 SQL。
        // 与自动确认任务撞上时只有一个会成功，另一个落到下面的状态错误
        if (this.baseMapper.confirmReceive(orderSn) == 0) {
            log.info("确认收货未生效，订单不是已发货状态：orderSn={}", orderSn);
            throw new BaseException(BaseCodeEnum.ORDER_STATUS_INVALID);
        }

        saveOperateHistory(order.getId(), OrderStatusEnum.RECIEVED.getCode(), user.getUsername(), "会员确认收货");
    }

    /** {@inheritDoc} */
    @Override
    public int autoConfirmReceive(int limit) {
        // 逐单独立更新，不合到一个事务里：某一单失败不该回滚已经确认好的其他订单
        int confirmed = 0;
        for (String orderSn : this.baseMapper.selectAutoConfirmable(limit)) {
            if (this.baseMapper.confirmReceive(orderSn) > 0) {
                confirmed++;
                // 操作史要订单 ID：选出来的是订单号，确认成功后再补一次查询。
                // 本方法不承担事务边界，状态更新与历史各自提交，历史写失败只影响这一条记录
                OrderEntity order = getOrderByOrderSn(orderSn);
                if (order != null) {
                    saveOperateHistory(order.getId(), OrderStatusEnum.RECIEVED.getCode(), "系统",
                            "发货已满自动确认天数，系统确认收货");
                }
            }
        }
        return confirmed;
    }

    /** {@inheritDoc} */
    @Override
    public List<DeliveryCompanyVo> listDeliveryCompanies() {
        return DeliveryCompanyEnum.listAll().stream()
                .map(company -> new DeliveryCompanyVo(company.getCode(), company.getName()))
                .toList();
    }

    /** {@inheritDoc} */
    @Override
    public List<OrderStatusOptionVo> listStatusOptions() {
        return Arrays.stream(OrderStatusEnum.values())
                .map(status -> new OrderStatusOptionVo(status.getCode(), status.getMsg()))
                .toList();
    }


    /** {@inheritDoc} */
    @Override
    public void createSeckillOrder(SeckillOrderTo orderTo) {

        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderSn(orderTo.getOrderSn());
        orderEntity.setMemberId(orderTo.getMemberId());
        orderEntity.setCreateTime(new Date());
        BigDecimal totalPrice = orderTo.getSeckillPrice().multiply(BigDecimal.valueOf(orderTo.getNum()));

        // 订单主表上的"可得积分/成长值"必须在落库前算好，赠送量还要按 spuId 取商品的积分设置
        R<SpuInfoVo> spuInfo = productFeignService.getSpuInfoBySkuId(orderTo.getSkuId());
        SpuInfoVo spuInfoData = spuInfo.getData();
        GiftCalculator.Amount gift = GiftCalculator.of(totalPrice, orderTo.getNum(),
                loadSkuBounds(List.of(orderTo.getSkuId())).get(orderTo.getSkuId()));

        // 与普通下单同一套口径：总额是商品金额、应付 = 总额 + 运费；
        // 秒杀单不收运费、不参与用券与积分抵扣，那几列写 0 而不是留 NULL
        orderEntity.setTotalAmount(totalPrice);
        orderEntity.setFreightAmount(BigDecimal.ZERO);
        orderEntity.setPayAmount(totalPrice);
        orderEntity.setCouponAmount(BigDecimal.ZERO);
        orderEntity.setPromotionAmount(BigDecimal.ZERO);
        orderEntity.setIntegrationAmount(BigDecimal.ZERO);
        // 秒杀单不参与积分抵扣，这一列也写 0 而不是留 NULL
        orderEntity.setUseIntegration(0);
        orderEntity.setIntegration(gift.integration());
        orderEntity.setGrowth(gift.growth());
        // memberUsername 留 NULL：秒杀消息里没有用户名，
        // member 的 Feign 也没有按 id 查会员的接口，取不到的值不猜
        orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        // 自动确认扫描按这两列选单，缺了就永远扫不到这一单
        orderEntity.setAutoConfirmDay(OrderConstant.AUTO_CONFIRM_DAY);
        orderEntity.setConfirmStatus(0);
        // 会员端列表按 delete_status = 0 过滤，不写就查不到这一单
        orderEntity.setDeleteStatus(0);
        // 秒杀单同样来自会员端 SPA；支付方式此时尚未选定，留空由支付回调写入
        orderEntity.setSourceType(OrderConstant.SOURCE_TYPE_PC);

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
        orderEntity.setReceiverPostCode(addressVo.getPostCode());

        this.save(orderEntity);
        // 秒杀消息里没有用户名，只有会员 ID，操作人按系统动作记
        saveOperateHistory(orderEntity.getId(), orderEntity.getStatus(), "系统（秒杀）", "秒杀下单");

        OrderItemEntity orderItem = new OrderItemEntity();
        // 订单已落库，自增主键此时才回填
        orderItem.setOrderId(orderEntity.getId());
        orderItem.setOrderSn(orderTo.getOrderSn());
        orderItem.setRealAmount(totalPrice);

        orderItem.setSkuQuantity(orderTo.getNum());

        orderItem.setSpuId(spuInfoData.getId());
        orderItem.setSpuName(spuInfoData.getSpuName());
        orderItem.setSpuPic(spuInfoData.getSpuPic());
        orderItem.setSpuBrand(spuInfoData.getBrandName());
        orderItem.setCategoryId(spuInfoData.getCatalogId());

        R<SkuInfoVo> skuInfo = productFeignService.getSkuInfoBySkuId(orderTo.getSkuId());
        SkuInfoVo skuInfoVo = skuInfo.getData();
        orderItem.setSkuId(orderTo.getSkuId());
        orderItem.setSkuName(skuInfoVo.getSkuName());
        orderItem.setSkuPic(skuInfoVo.getSkuDefaultImg());
        orderItem.setSkuPrice(skuInfoVo.getPrice());
        orderItem.setPromotionAmount(new BigDecimal(0));
        orderItem.setCouponAmount(new BigDecimal(0));
        orderItem.setIntegrationAmount(new BigDecimal(0));
        // 与普通下单同一套口径：基础量取原价总额的 10% 向下取整，再加商品上配置的积分设置
        // 取整用 intValue 而不是 intValueExact：后者遇到带小数的秒杀价会抛异常，失败即无限重投
        orderItem.setGiftGrowth(gift.growth());
        orderItem.setGiftIntegration(gift.integration());
        orderItemService.save(orderItem);
    }

    /** {@inheritDoc} */
    @Override
    public CommentTargetVo getCommentTarget(Long orderItemId, Long memberId) {
        OrderItemEntity item = orderItemService.getById(orderItemId);
        if (item == null) {
            throw new BaseException(BaseCodeEnum.COMMENT_ORDER_ITEM_NOT_FOUND);
        }

        OrderEntity order = getOrderByOrderSn(item.getOrderSn());
        // 订单查不到与"不属于该会员"合并成同一个码：分开等于给出一个"订单项存在但不属于你"的枚举点
        if (order == null || !memberId.equals(order.getMemberId())) {
            throw new BaseException(BaseCodeEnum.COMMENT_ORDER_ITEM_NOT_FOUND);
        }
        if (!OrderStatusEnum.RECIEVED.getCode().equals(order.getStatus())) {
            throw new BaseException(BaseCodeEnum.COMMENT_ORDER_NOT_FINISHED);
        }

        CommentTargetVo vo = new CommentTargetVo();
        vo.setOrderSn(order.getOrderSn());
        vo.setOrderItemId(item.getId());
        vo.setSkuId(item.getSkuId());
        vo.setSpuId(item.getSpuId());
        vo.setSpuName(item.getSpuName());
        // 销售属性在下单时快照进订单项，评价行沿用同一份，不回头查商品
        vo.setSpuAttributes(item.getSkuAttrsVals());
        vo.setMemberUsername(order.getMemberUsername());
        return vo;
    }

    /** {@inheritDoc} */
    @Override
    public void markCommented(String orderSn, Long memberId) {
        if (this.baseMapper.markCommented(orderSn, memberId) == 0) {
            // 不抛异常：评价的事实已经在 product 侧落定，这里只补展示时间列与一条操作史，
            // 重复登记、越权或状态不符都不该让调用方重试
            log.info("评价时间未登记，订单不存在、不属于该会员、不是已完成状态或已登记过：orderSn={}", orderSn);
            return;
        }

        OrderEntity order = getOrderByOrderSn(orderSn);
        if (order != null) {
            // 秒杀单的 member_username 为空，用会员 ID 兜底：操作人留空就看不出这条记录是谁做的
            String operateMan = order.getMemberUsername() != null
                    ? order.getMemberUsername() : "会员#" + memberId;
            saveOperateHistory(order.getId(), order.getStatus(), operateMan, "会员评价");
        }
    }

    /** {@inheritDoc} */
    @Override
    public void unmarkCommented(Long orderItemId, Long memberId) {
        OrderItemEntity item = orderItemService.getById(orderItemId);
        if (item == null) {
            // 订单项已经不存在（比如订单被硬删）：没有订单时间可撤，按幂等处理
            log.info("撤回评价时间跳过，订单项不存在：orderItemId={}", orderItemId);
            return;
        }
        OrderEntity order = getOrderByOrderSn(item.getOrderSn());
        if (order == null) {
            log.info("撤回评价时间跳过，订单不存在：orderSn={}", item.getOrderSn());
            return;
        }

        // 带 member_id 与 status 条件：撤别人的、撤未完成订单的都不会改到行
        if (this.baseMapper.unmarkCommented(item.getOrderSn(), memberId) == 0) {
            log.info("评价时间未撤回，不属于该会员、不是已完成状态或本来就没有评价时间：orderSn={}",
                    item.getOrderSn());
            return;
        }

        // 秒杀单的 member_username 为空，用会员 ID 兜底：操作人留空就看不出这条记录是谁做的
        String operateMan = order.getMemberUsername() != null
                ? order.getMemberUsername() : "会员#" + memberId;
        saveOperateHistory(order.getId(), order.getStatus(), operateMan, "会员删除评价");
    }

}

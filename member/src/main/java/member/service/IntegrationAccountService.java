package member.service;

/**
 * 会员积分账户的变动入口，供订单链路在算价、下单抵扣、关单退回与支付赠送四个时机调用。
 *
 * <p>实现方必须保证：扣减在余额不足时不改动任何数据；同一 {@code orderSn} 的同一类变动
 * 重复调用只落账一次（幂等），调用方因此可以直接重试而不必自己去重。
 */
public interface IntegrationAccountService {

    /**
     * 查询会员当前的积分余额。
     *
     * <p>无副作用。积分列允许为 {@code null}，实现方必须归一到 0，不返回 {@code null}。
     *
     * @param memberId 会员 id，不能为 {@code null}
     * @return 可用积分，最小为 0
     * @throws common.exception.BaseException 会员不存在时抛出，错误码 15002
     */
    int balance(Long memberId);

    /**
     * 下单时抵扣积分，把余额减去指定数量。
     *
     * <p>幂等：同一 {@code orderSn} 重复调用只扣一次，第二次起按成功返回。
     *
     * @param memberId 会员 id，不能为 {@code null}
     * @param orderSn 订单号，必填；它是幂等判据，也是会员端流水的来源凭据
     * @param points 抵扣数量，必须为正数
     * @throws common.exception.BaseException 余额不足（15009）或会员不存在（15002）时抛出
     */
    void deduct(Long memberId, String orderSn, int points);

    /**
     * 关单时退回已抵扣的积分，把余额加上指定数量。
     *
     * <p>幂等：关单消息会重投，重复调用只退一次，第二次起按成功返回。
     *
     * @param memberId 会员 id，不能为 {@code null}
     * @param orderSn 订单号，必填
     * @param points 退回数量，必须为正数
     * @throws common.exception.BaseException 会员不存在（15002）时抛出
     */
    void refund(Long memberId, String orderSn, int points);

    /**
     * 支付成功时赠送积分，把余额加上指定数量。
     *
     * <p>幂等：支付回调会重投，重复调用只赠一次，第二次起按成功返回。
     *
     * @param memberId 会员 id，不能为 {@code null}
     * @param orderSn 订单号，必填
     * @param points 赠送数量，必须为正数
     * @throws common.exception.BaseException 会员不存在（15002）时抛出
     */
    void grant(Long memberId, String orderSn, int points);
}

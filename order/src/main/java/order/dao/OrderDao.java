package order.dao;

import order.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;


import java.util.List;

/**
 * {@code oms_order} 表的 MyBatis-Plus Mapper，映射订单主表 {@link OrderEntity}。
 *
 * <p>自定义 SQL 有四条状态流转：{@link #updateOrderStatus} 服务于支付，{@link #deliverOrder}
 * 服务于发货，{@link #confirmReceive} 服务于确认收货，{@link #closeOrder} 服务于关单与取消；
 * 另有 {@link #updateNote} 服务于后台改备注、{@link #markDeleted} 服务于会员软删、
 * {@link #deleteMarked} 服务于后台硬删、{@link #markCommented} 服务于评价后登记评价时间、
 * {@link #unmarkCommented} 服务于删除评价后撤回评价时间、
 * {@link #selectAutoConfirmable} 供自动确认任务扫描到期订单，
 * 其余使用 {@link BaseMapper} 提供的通用增删改查。
 */
@Mapper
public interface OrderDao extends BaseMapper<OrderEntity> {

    /**
     * 支付：把订单从"待付款"改成已付款，并写入支付方式与支付时间。
     *
     * <p>SQL 见 {@code mapper/order/OrderDao.xml}：{@code WHERE} 带 {@code status = 0}，
     * 判断与写入是同一条 SQL，因此重复回调、或已被关单的订单只会更新 0 行。
     * {@code modify_time} 与 {@code payment_time} 总是被置为当前时间，与传入的 {@code code} 无关，
     * 因此只适用于支付相关的状态流转。
     *
     * @param orderSn 订单号，业务主键，不能为 {@code null}
     * @param code 目标状态码，取值见 {@link order.enums.OrderStatusEnum}
     * @param payType 支付方式，取值见 {@code PayConstant}
     * @return 受影响行数；1 表示更新生效，0 表示订单不存在或已不是待付款状态
     */
    int updateOrderStatus(String orderSn, Integer code, Integer payType);

    /**
     * 发货：把订单从"已付款"改成"已发货"，并写入物流公司与物流单号。
     *
     * <p>SQL 见 {@code mapper/order/OrderDao.xml}：目标状态与前置状态都固化在语句里，
     * {@code WHERE} 带 {@code status = 1}，判断与写入是同一条 SQL，因此并发点两次发货只有一个会成功。</p>
     *
     * <p><b>刻意不写 {@code payment_time} 也不写 {@code pay_type}</b>，与 {@link #updateOrderStatus}
     * 分开：那条语句总把两个支付字段置为当前值，拿来做发货会把发货时间写成付款时间、
     * 并覆盖掉真实的支付方式。</p>
     *
     * @param orderSn 订单号，业务主键，不能为 {@code null}
     * @param deliveryCompany 物流公司（配送方式），不能为 {@code null}
     * @param deliverySn 物流单号，不能为 {@code null}
     * @return 受影响行数，1 表示发货成功，0 表示订单不存在或不是已付款状态
     */
    int deliverOrder(String orderSn, String deliveryCompany, String deliverySn);

    /**
     * 关单：把订单从"待付款"改成已取消。
     *
     * <p>SQL 见 {@code mapper/order/OrderDao.xml}：{@code WHERE} 带 {@code status = 0}，
     * 判断与写入是同一条 SQL，因此与支付回调并发时只有一个会成功。
     *
     * @param orderSn 订单号，业务主键，不能为 {@code null}
     * @return 受影响行数，1 表示关单生效，0 表示订单不存在或已不是待付款状态
     */
    int closeOrder(String orderSn);

    /**
     * 确认收货：把订单从"已发货"改成已完成，并写入确认状态与确认收货时间。
     *
     * <p>SQL 见 {@code mapper/order/OrderDao.xml}：{@code WHERE} 带 {@code status = 2}，
     * 判断与写入是同一条 SQL，因此会员手动确认与任务自动确认并发时只有一个会成功。
     *
     * @param orderSn 订单号，业务主键，不能为 {@code null}
     * @return 受影响行数，1 表示确认生效，0 表示订单不存在或已不是已发货状态
     */
    int confirmReceive(String orderSn);

    /**
     * 会员删除自己的订单：把订单标记为已删除。
     *
     * <p>SQL 见 {@code mapper/order/OrderDao.xml}：{@code WHERE} 带 {@code member_id}、状态与
     * {@code delete_status = 0}，归属、状态与"未删除"三个判断和写入是同一条 SQL，
     * 删别人的单、删进行中的单、重复删除都只会更新 0 行。</p>
     *
     * @param orderSn 订单号，业务主键，不能为 {@code null}
     * @param memberId 当前登录会员 ID，不能为 {@code null}
     * @return 受影响行数，1 表示删除生效，0 表示订单不存在、不属于该会员、状态不允许或已删除
     */
    int markDeleted(String orderSn, Long memberId);

    /**
     * 后台硬删订单：从库里删掉订单主表的一行。
     *
     * <p>SQL 见 {@code mapper/order/OrderDao.xml}：{@code WHERE} 带 {@code delete_status = 1}，
     * 没被会员标记过的订单删不掉。物理删除不可逆，准入条件由 SQL 说了算而不是只在服务层判断。</p>
     *
     * <p>本方法只删订单主表：订单项、支付流水与库存工作单由调用方级联清理。</p>
     *
     * @param orderSn 订单号，业务主键，不能为 {@code null}
     * @return 受影响行数，1 表示已删除，0 表示订单不存在或没被标记过
     */
    int deleteMarked(String orderSn);

    /**
     * 修改订单备注。
     *
     * <p>SQL 见 {@code mapper/order/OrderDao.xml}：只写 {@code note} 与 {@code modify_time}，
     * 不带状态条件 —— 备注是给运营看的说明，不参与状态流转，任何状态的订单都能改。
     *
     * @param orderSn 订单号，业务主键，不能为 {@code null}
     * @param note 订单备注，可为 {@code null} 或空串，表示清空
     * @return 受影响行数，1 表示更新生效，0 表示订单不存在
     */
    int updateNote(String orderSn, String note);

    /**
     * 查出已经越过自动确认期限的订单号，供自动确认任务逐单确认。
     *
     * <p>SQL 见 {@code mapper/order/OrderDao.xml}：期限按发货时间加 {@code auto_confirm_day} 天算，
     * 比较在数据库里完成，不受应用时区影响。{@code auto_confirm_day} 为空的历史订单不会被选出。
     *
     * @param limit 单轮最多取多少条，必须为正数
     * @return 到期可自动确认的订单号；没有到期订单时返回空列表
     */
    List<String> selectAutoConfirmable(int limit);

    /**
     * 登记订单的评价时间。
     *
     * <p>SQL 见 {@code mapper/order/OrderDao.xml}：{@code WHERE} 带 {@code member_id}、{@code status = 3}
     * 与 {@code comment_time IS NULL}，归属、状态与"还没登记过"三个判断和写入是同一条 SQL，
     * 越权登记、对未完成的订单登记、重复登记都只会更新 0 行。</p>
     *
     * <p>与几条状态流转分开：它不碰 {@code status}，只补 {@code comment_time} 与 {@code modify_time}。
     * 评价本身落在 product 侧，本条语句只登记订单自己的展示时间。</p>
     *
     * @param orderSn 订单号，业务主键，不能为 {@code null}
     * @param memberId 当前登录会员 ID，不能为 {@code null}
     * @return 受影响行数，1 表示登记生效，0 表示订单不存在、不属于该会员、不是已完成状态或已登记过
     */
    int markCommented(String orderSn, Long memberId);

    /**
     * 撤回订单的评价时间。
     *
     * <p>SQL 见 {@code mapper/order/OrderDao.xml}：{@code WHERE} 带 {@code member_id} 与
     * {@code status = 3}，归属与状态校验跟写入是同一条 SQL。与 {@link #markCommented} 的区别是
     * 不要求 {@code comment_time} 非空 —— 撤回本身就是把值清掉，重复调用只会更新 0 行。</p>
     *
     * <p>只写 {@code comment_time} 与 {@code modify_time}，不碰 {@code status}。</p>
     *
     * @param orderSn 订单号，业务主键，不能为 {@code null}
     * @param memberId 当前登录会员 ID，不能为 {@code null}
     * @return 受影响行数，1 表示撤回生效，0 表示订单不存在、不属于该会员、不是已完成状态或本来就没有评价时间
     */
    int unmarkCommented(String orderSn, Long memberId);

    /**
     * 对订单行加排他锁，并把锁住的那一行带回来。
     *
     * <p><b>谁用它、为什么：</b>售后有两处必须先锁住订单行。
     * 一是<b>提交申请前</b>——判据是"同一张订单不许同时存在两笔进行中的售后"，先查后插在并发下会各自
     * 查到"没有"、双双插入，后台就可能把同一笔退款批两次、退两次钱。锁住订单行让同一订单的申请排队执行。
     * 二是<b>整单退款前</b>——退款要"先问平台拿到钱确实退了的应答、再作废订单"，中间不能让发货之类
     * 的操作把订单状态改掉：不然钱已经退出去，那条 {@code WHERE status = 1} 的作废语句会改 0 行，
     * 整个事务回滚，留下"钱退了、账没有"。
     *
     * <p><b>返回值必须是这一条语句查出来的行</b>：调用方要按锁住那一刻的状态做判断，
     * 而这之前用其它语句读到的实体可能来自 MyBatis 的会话缓存（同一事务内同一条查询会命中缓存），
     * 拿它判断等于在锁外判断。
     *
     * <p>调用方还必须跑在 {@code READ_COMMITTED} 下 —— 默认的 {@code REPEATABLE READ}
     * 会把锁之后那次普通 SELECT 钉在事务开始时的快照上，读不到前一个事务刚提交的插入，闸门等于失效
     * （与券的限领判定是同一个坑，见 {@code coupon} 侧那两条发券路径）。
     *
     * <p>本方法只取锁、不改数据。</p>
     *
     * @param orderSn 订单号，不能为 {@code null}
     * @return 锁住的那一刻的订单行；订单不存在时返回 {@code null}
     */
    OrderEntity lockOrderForAfterSale(String orderSn);

    /**
     * 整单退款成功后的订单作废：把订单从"已付款"改成已取消。
     *
     * <p>与 {@link #closeOrder} 分开，两条语句只差一个前置状态：关单那条从"待付款"来（用户自己取消、
     * 超时没付、后台作废），这条从"已付款"来（钱收过又退回去）。合成一条会让"待付款"与"已付款"
     * 两种情况共用一次状态判断，读的人无法确认它到底允许从哪来。</p>
     *
     * <p>带 {@code status = 1} 条件：与发货并发时只有一个会成功 —— 发货赢了说明货已经出去，
     * 这一单不该再作废。</p>
     *
     * @param orderSn 订单号，业务主键，不能为 {@code null}
     * @return 受影响行数，1 表示作废生效，0 表示订单不存在或不是已付款状态
     */
    int refundCloseOrder(String orderSn);
}

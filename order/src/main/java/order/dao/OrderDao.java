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
 * 另有 {@link #updateNote} 服务于后台改备注、{@link #selectAutoConfirmable} 供自动确认任务扫描到期订单，
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
}

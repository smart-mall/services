package order.dao;

import order.entity.OrderReturnApplyEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * {@code oms_order_return_apply} 表的 MyBatis-Plus Mapper，映射售后申请 {@link OrderReturnApplyEntity}。
 *
 * <p>四条自定义 SQL 都是售后申请的状态流转，每条都把"允许从哪个状态来"与"目标状态"固化在语句里，
 * 判断与写入是同一条 SQL：重复审核、重复确认收货、没通过审核就收货，都只会更新 0 行。
 * 插入用 {@link BaseMapper} 的通用方法，并发由调用方对订单行加的排他锁兜住。
 */
@Mapper
public interface OrderReturnApplyDao extends BaseMapper<OrderReturnApplyEntity> {

    /**
     * 退货审核通过：待处理 0 → 退货中 1，并把商家的退货地址与收货人写进申请单。
     *
     * <p>SQL 见 {@code mapper/order/OrderReturnApplyDao.xml}：{@code WHERE} 带 {@code type = 1}
     * 与 {@code status = 0}，所以对仅退款的单、对已经处理过的单都更新 0 行。</p>
     *
     * <p>退货地址由服务端填（{@code OrderConstant} 里的固定值），不由后台每次手填。</p>
     *
     * @param id 申请单主键，不能为 {@code null}
     * @param handleMan 处理人（管理员用户名），不能为 {@code null}
     * @param companyAddress 商家的退货地址，不能为 {@code null}
     * @param receiveMan 商家的退货收货人，不能为 {@code null}
     * @param receivePhone 商家的退货收货电话，不能为 {@code null}
     * @param handleNote 处理备注，可为 {@code null}
     * @return 受影响行数，1 表示流转生效，0 表示不是待处理的退货单
     */
    int approveReturning(Long id, String handleMan, String companyAddress, String receiveMan,
                         String receivePhone, String handleNote);

    /**
     * 仅退款通过：待处理 0 → 已完成 2。
     *
     * <p>SQL 见 {@code mapper/order/OrderReturnApplyDao.xml}：{@code WHERE} 带 {@code type = 0}
     * 与 {@code status = 0}。不经过"退货中"—— 货没发出去，没有等会员寄回这一段。</p>
     *
     * <p>本方法只改申请单；订单作废、库存解锁、退券与退积分由调用方在同一事务里接着做。</p>
     *
     * @param id 申请单主键，不能为 {@code null}
     * @param handleMan 处理人（管理员用户名），不能为 {@code null}
     * @param handleNote 处理备注，可为 {@code null}
     * @return 受影响行数，1 表示流转生效，0 表示不是待处理的退款单
     */
    int finishRefund(Long id, String handleMan, String handleNote);

    /**
     * 商家确认收到退货：退货中 1 → 已完成 2，并写入收货人、收货时间与收货备注。
     *
     * <p>SQL 见 {@code mapper/order/OrderReturnApplyDao.xml}：{@code WHERE} 带 {@code type = 1}
     * 与 {@code status = 1}，没通过审核的单收不了货、重复点也只会更新 0 行。</p>
     *
     * @param id 申请单主键，不能为 {@code null}
     * @param receiveMan 商家收货人，不能为 {@code null}
     * @param receiveNote 收货备注，可为 {@code null}
     * @return 受影响行数，1 表示流转生效，0 表示不是"退货中"的单
     */
    int finishReturn(Long id, String receiveMan, String receiveNote);

    /**
     * 拒绝售后申请：待处理 0 → 已拒绝 3，两条链共用。
     *
     * <p>只认待处理：已经通过的退货单不能再拒绝 —— 那时货可能已经在路上，把状态改成"已拒绝"
     * 会让会员钱货两空。</p>
     *
     * @param id 申请单主键，不能为 {@code null}
     * @param handleMan 处理人（管理员用户名），不能为 {@code null}
     * @param handleNote 驳回理由，调用方保证非空
     * @return 受影响行数，1 表示流转生效，0 表示不是待处理的申请
     */
    int rejectApply(Long id, String handleMan, String handleNote);
}

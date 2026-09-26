package order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import order.entity.PaymentInfoEntity;

import java.util.Map;

import common.query.PageQuery;
/**
 * 支付信息服务：在通用 CRUD 之上提供后台列表的分页查询。
 */
public interface PaymentInfoService extends IService<PaymentInfoEntity> {

    /**
     * 分页查询支付流水，供后台列表使用。
     *
     * <p>没有业务筛选条件，返回全部支付记录。
     *
     * @param query 分页参数，不能为 {@code null}
     * @return 分页结果，{@code rows} 为当前页支付流水；当前页没有数据时为空列表
     */
    PageVO<PaymentInfoEntity> queryPage(PageQuery query);
}


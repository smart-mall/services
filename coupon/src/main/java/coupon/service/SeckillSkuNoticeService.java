package coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import coupon.entity.SeckillSkuNoticeEntity;


import common.query.PageQuery;
/**
 * 秒杀商品通知订阅，记录用户对某个秒杀商品的到点提醒。
 *
 * <p>继承 {@link IService}，通用增删改查由 MyBatis-Plus 提供；本接口只补管理端的分页查询。
 */
public interface SeckillSkuNoticeService extends IService<SeckillSkuNoticeEntity> {

    /**
     * 分页查询秒杀商品通知订阅。
     *
     * <p>不带筛选条件，返回全部记录；实现方不保证行序。
     *
     * @param query 分页参数，不能为 {@code null}；{@code page} / {@code limit} 非法时取默认值并截断上限
     * @return 分页结果，无数据时 {@code rows} 为空列表、{@code total} 为 0，不返回 {@code null}
     */
    PageVO<SeckillSkuNoticeEntity> queryPage(PageQuery query);
}


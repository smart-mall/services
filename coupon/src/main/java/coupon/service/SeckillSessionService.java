package coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import coupon.entity.SeckillSessionEntity;

import java.util.List;
import java.util.Map;

import common.query.KeyPageQuery;
/**
 * 秒杀活动场次，每个场次有独立的开始与结束时间。
 *
 * <p>继承 {@link IService}，通用增删改查由 MyBatis-Plus 提供；本接口补管理端分页查询，
 * 以及 seckill 服务定时上架所需的场次装配查询。
 */
public interface SeckillSessionService extends IService<SeckillSessionEntity> {

    /**
     * 分页查询秒杀场次，{@code key} 模糊匹配场次名，或按场次 ID 精确匹配。
     *
     * <p>{@code key} 为空时不加筛选条件，返回全部场次。
     *
     * @param query 分页参数与关键字，不能为 {@code null}；{@code key} 可以为 {@code null} 或空白
     * @return 分页结果，无数据时 {@code rows} 为空列表、{@code total} 为 0，不返回 {@code null}
     */
    PageVO<SeckillSessionEntity> queryPage(KeyPageQuery query);

    /**
     * 查询开始时间落在今天 00:00:00 至后天 23:59:59 之间的秒杀场次，并装配每场关联的秒杀商品。
     *
     * <p>关联商品按场次逐个查询，场次越多 SQL 次数越多；结果中的场次对象会被就地填充
     * {@code relationSkus}。
     *
     * @return 场次列表，每项的 {@code relationSkus} 已填充；没有符合条件的场次时返回 {@code null}，
     *         不返回空列表，调用方必须判空
     */
    List<SeckillSessionEntity> getLates3DaySession();
}


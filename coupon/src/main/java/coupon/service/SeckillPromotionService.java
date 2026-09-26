package coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import coupon.entity.SeckillPromotionEntity;

import java.util.Map;

import common.query.KeyPageQuery;
/**
 * 秒杀活动。
 *
 * <p>继承 {@link IService}，通用增删改查由 MyBatis-Plus 提供；本接口只补管理端的分页查询。
 */
public interface SeckillPromotionService extends IService<SeckillPromotionEntity> {

    /**
     * 分页查询秒杀活动，{@code key} 同时模糊匹配活动标题与活动 ID，并回填创建人名称。
     *
     * <p>{@code key} 为空时不加筛选条件，返回全部活动。远程映射里缺失的创建人 ID，
     * 其 {@code userName} 为 {@code null}。
     *
     * @param query 分页参数与关键字，不能为 {@code null}；{@code key} 可以为 {@code null} 或空白
     * @return 分页结果，无数据时 {@code rows} 为空列表、{@code total} 为 0，不返回 {@code null}
     * @throws BaseException 远程获取用户名失败时抛出
     */
    PageVO<SeckillPromotionEntity> queryPage(KeyPageQuery query);
}


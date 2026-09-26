package coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import coupon.entity.SeckillSkuRelationEntity;

import java.util.Map;

import coupon.vo.SeckillSkuRelationPageQuery;
/**
 * 秒杀活动商品关联，记录每场秒杀里参与的商品及其秒杀价、限购数。
 *
 * <p>继承 {@link IService}，通用增删改查由 MyBatis-Plus 提供；本接口只补管理端的分页查询。
 */
public interface SeckillSkuRelationService extends IService<SeckillSkuRelationEntity> {

    /**
     * 按秒杀场次分页查询场次内的秒杀商品。
     *
     * <p>{@code query.promotionSessionId} 为 {@code null} 或空白时不加筛选条件，返回全部关联记录。
     *
     * @param query 分页参数与场次 ID，不能为 {@code null}；{@code promotionSessionId} 可以为 {@code null} 或空白
     * @return 分页结果，无数据时 {@code rows} 为空列表、{@code total} 为 0，不返回 {@code null}
     */
    PageVO<SeckillSkuRelationEntity> queryPage(SeckillSkuRelationPageQuery query);
}


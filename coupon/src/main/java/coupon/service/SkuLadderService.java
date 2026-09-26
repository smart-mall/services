package coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import coupon.entity.SkuLadderEntity;


import common.query.PageQuery;
/**
 * 商品阶梯价格，对应 {@code sms_sku_ladder} 表。
 *
 * <p>继承 {@link IService}，通用增删改查由 MyBatis-Plus 提供；本接口只补管理端的分页查询。
 */
public interface SkuLadderService extends IService<SkuLadderEntity> {

    /**
     * 分页查询商品阶梯价格。
     *
     * <p>不带筛选条件，返回全部记录；实现方不保证行序。
     *
     * @param query 分页参数，不能为 {@code null}；{@code page} / {@code limit} 非法时取默认值并截断上限
     * @return 分页结果，无数据时 {@code rows} 为空列表、{@code total} 为 0，不返回 {@code null}
     */
    PageVO<SkuLadderEntity> queryPage(PageQuery query);
}


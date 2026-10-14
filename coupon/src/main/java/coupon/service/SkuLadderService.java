package coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import coupon.entity.SkuLadderEntity;


import common.query.PageQuery;
/**
 * 商品阶梯价格，对应 {@code sms_sku_ladder} 表。
 *
 * <p>继承 {@link IService}，通用增删改查由 MyBatis-Plus 提供；本接口补管理端的分页查询与覆盖保存。
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

    /**
     * 保存一条阶梯价，同一个 SKU 只留一行。
     *
     * <p>按 {@code skuId} 覆盖而不是追加，与商品保存路径同一套写法：否则后台连点两次新增
     * 就会给同一个 SKU 造出两条阶梯价，算价时两条都命中。
     *
     * @param skuLadder 阶梯价内容，{@code skuId} 必填；不能为 {@code null}
     * @throws common.exception.BaseException {@code skuId} 为空时抛出
     */
    void saveLadder(SkuLadderEntity skuLadder);
}


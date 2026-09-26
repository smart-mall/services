package coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.to.SkuReductionTo;
import common.vo.PageVO;
import coupon.entity.SkuFullReductionEntity;

import java.util.List;
import java.util.Map;

import common.query.KeyPageQuery;
/**
 * 商品满减信息，对应 {@code sms_sku_full_reduction} 表。
 *
 * <p>继承 {@link IService}，通用增删改查由 MyBatis-Plus 提供；本接口补管理端分页查询，
 * 以及发布商品时一次写入满减、阶梯价、会员价三张表的入口。
 */
public interface SkuFullReductionService extends IService<SkuFullReductionEntity> {

    /**
     * 分页查询商品满减信息，并回填 SKU 名称。
     *
     * <p>{@code key} 为空时直接返回当前页；非空时在当前页内按行 ID 或远程取回的 SKU 名称过滤，
     * 因此 {@code total} 是过滤前的总行数，可能大于 {@code rows} 的规模。远程映射里缺失的 ID，
     * 其 {@code skuName} 为 {@code null}。
     *
     * @param query 分页参数与关键字，不能为 {@code null}；{@code key} 可以为 {@code null} 或空白
     * @return 分页结果，无数据时 {@code rows} 为空列表、{@code total} 为 0，不返回 {@code null}
     * @throws BaseException 远程获取 SKU 名称失败时抛出
     */
    PageVO<SkuFullReductionEntity> queryPage(KeyPageQuery query);

    /**
     * 保存一个 SKU 的优惠信息，按入参条件最多写三张表。
     *
     * <p>{@code fullCount} 大于 0 时写阶梯价（{@code sms_sku_ladder}），{@code fullPrice} 大于 0 时
     * 写满减（{@code sms_sku_full_reduction}），{@code memberPrice} 非空时把其中价格大于 0 的条目
     * 写会员价（{@code sms_member_price}）；三张表的写入在同一事务内，一起成功或一起回滚。
     *
     * <p>非幂等：全部是 INSERT，没有按 {@code skuId} 覆盖或先去重，重复调用会产生重复行。
     *
     * @param skuReductionTo 优惠信息，不能为 {@code null}；{@code skuId}、{@code fullCount}、
     *                       {@code fullPrice} 也不能为 {@code null}，{@code memberPrice} 可以为 {@code null}
     */
    void saveSkuReduction(SkuReductionTo skuReductionTo);

    /**
     * 按 skuId 批量删除满减、阶梯价与会员价三张表中的记录。
     *
     * <p>与 {@link #saveSkuReduction(SkuReductionTo)} 的写入路径对称。商品删除后由
     * {@link ProductCleanupService} 本地调用，三条 DELETE 在同一事务内；纯删除天然幂等，
     * 重复调用不会出错。
     *
     * @param skuIds 商品 SKU 标识集合；为 {@code null} 或空集合时不做任何操作，直接返回
     */
    void deleteBySkuIds(List<Long> skuIds);
}


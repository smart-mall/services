package coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import coupon.entity.SpuBoundsEntity;

import java.util.List;
import java.util.Map;

import common.query.KeyPageQuery;
/**
 * 商品 SPU 积分设置，对应 {@code sms_spu_bounds} 表。
 *
 * <p>继承 {@link IService}，通用增删改查由 MyBatis-Plus 提供；本接口补管理端分页查询与按 spuId 批量删除。
 */
public interface SpuBoundsService extends IService<SpuBoundsEntity> {

    /**
     * 分页查询商品 SPU 积分设置，并回填 SPU 名称。
     *
     * <p>{@code key} 为空时直接返回当前页；非空时在当前页内按行 ID 或远程取回的 SPU 名称过滤，
     * 因此 {@code total} 是过滤前的总行数，可能大于 {@code rows} 的规模。远程映射里缺失的 ID，
     * 其 {@code spuName} 为 {@code null}。
     *
     * @param query 分页参数与关键字，不能为 {@code null}；{@code key} 可以为 {@code null} 或空白
     * @return 分页结果，无数据时 {@code rows} 为空列表、{@code total} 为 0，不返回 {@code null}
     * @throws BaseException 远程获取 SPU 名称失败时抛出
     */
    PageVO<SpuBoundsEntity> queryPage(KeyPageQuery query);

    /**
     * 按 spuId 批量删除积分设置。
     *
     * <p>商品删除后由 {@link ProductCleanupService} 本地调用。删除条件必须是 spuId 而不是行主键 id
     * —— 调用方手里只有商品 SPU 标识，没有这张表的行 id。纯删除天然幂等，重复调用不会出错。
     *
     * @param spuIds 商品 SPU 标识集合；为 {@code null} 或空集合时不做任何操作，直接返回
     */
    void deleteBySpuIds(List<Long> spuIds);
}


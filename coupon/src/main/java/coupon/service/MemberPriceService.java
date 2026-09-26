package coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import coupon.entity.MemberPriceEntity;

import java.util.Map;

import common.query.KeyPageQuery;
/**
 * 商品会员价格，按会员等级维护同一个 SKU 的不同价格。
 *
 * <p>继承 {@link IService}，通用增删改查由 MyBatis-Plus 提供；本接口只补管理端的分页查询。
 */
public interface MemberPriceService extends IService<MemberPriceEntity> {

    /**
     * 分页查询商品会员价格，并回填 SKU 名称。
     *
     * <p>{@code key} 为空时直接返回当前页；非空时在当前页内按行 ID 或远程取回的 SKU 名称过滤，
     * 因此 {@code total} 是过滤前的总行数，可能大于 {@code rows} 的规模。远程映射里缺失的 ID，
     * 其 {@code skuName} 为 {@code null}。
     *
     * @param query 分页参数与关键字，不能为 {@code null}；{@code key} 可以为 {@code null} 或空白
     * @return 分页结果，无数据时 {@code rows} 为空列表、{@code total} 为 0，不返回 {@code null}
     * @throws BaseException 远程获取 SKU 名称失败时抛出
     */
    PageVO<MemberPriceEntity> queryPage(KeyPageQuery query);
}


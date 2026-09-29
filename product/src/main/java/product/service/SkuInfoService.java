package product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.to.SkuScopeVo;
import common.vo.PageVO;
import product.entity.SkuInfoEntity;
import product.vo.SkuItemVo;
import product.vo.SkuSelectVO;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import common.query.PageQuery;
import product.vo.SkuInfoPageQuery;
/**
 * sku 服务：维护 sku 主表，并组装商品详情页所需的全部数据。
 *
 * <p>sku 的生命周期由 spu 管理，没有单独的删除入口。
 */
public interface SkuInfoService extends IService<SkuInfoEntity> {

    /**
     * 分页查询全部 sku，不带任何筛选条件。
     *
     * @param query 分页参数，{@code page} 为页码、{@code limit} 为每页条数，不能为 {@code null}
     * @return 分页结果，{@code rows} 为 sku 列表
     */
    PageVO<SkuInfoEntity> queryPage(PageQuery query);

    /**
     * 按条件分页查询 sku。
     *
     * @param query 查询条件：{@code key} 匹配 sku ID 或 sku 名；{@code catalogId}、{@code brandId}
     *              为空或 0 时不参与过滤；{@code min} 与 {@code max} 同时给出且 {@code min < max}
     *              时才按价格区间过滤；不能为 {@code null}
     * @return 分页结果，{@code rows} 为 sku 列表
     */
    PageVO<SkuInfoEntity> queryPageByCondition(SkuInfoPageQuery query);

    /**
     * 组装商品详情页所需的全部数据：sku 基本信息、图集、销售属性、商品介绍、规格参数、
     * 秒杀优惠与是否有货。
     *
     * <p>基本信息之外的各数据块在线程池里并行加载：库存服务异常只记日志、保留默认「有货」，
     * 秒杀信息非 0 码按无秒杀处理。
     *
     * @param skuId sku ID，不能为 {@code null}
     * @return 商品详情；sku 不存在时只返回一个 {@code info} 为 {@code null} 的对象，其余字段保持默认值
     * @throws ExecutionException 并行加载任务执行失败时抛出，由全局异常处理器兜住
     * @throws InterruptedException 等待并行任务时当前线程被中断
     */
    SkuItemVo item(Long skuId) throws ExecutionException, InterruptedException;

    /**
     * 返回全部 sku 的 ID 与名称，供下拉框选择。
     *
     * <p>一次查出全表，不分页也不过滤。
     *
     * @return sku 选择项列表；没有 sku 时返回空列表
     */
    List<SkuSelectVO> getSkuSelect();

    /**
     * 按 sku ID 批量取 sku 名称。
     *
     * @param spuIds sku ID 列表，不能为 {@code null}；参数名沿用调用方的写法，实际按 sku 主键查询
     * @return skuId 到 sku 名称的映射；入参里查不到的 ID 不会出现在结果中
     */
    Map<Long, String> getUserNames(List<Long> spuIds);

    /**
     * 按 sku ID 批量取它在商品层级里的归属（所属 SPU 与分类）。
     *
     * <p>给下游做"指定商品 / 指定分类"的规则匹配用：调用方只拿得到购物车里的 skuId，
     * 逐条回查商品信息会退化成 N 次远程调用，所以一次批量给出。
     *
     * @param skuIds sku ID 列表，不能为 {@code null}，可以为空集合
     * @return skuId 到归属信息的映射；空入参或查不到的 ID 不会出现在结果中，不返回 {@code null}
     */
    Map<Long, SkuScopeVo> getSkuScopes(List<Long> skuIds);
}


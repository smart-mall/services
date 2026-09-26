package product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import product.entity.SpuInfoEntity;
import product.vo.SpuSelectVO;
import product.vo.SpuVO;

import java.util.List;
import java.util.Map;

import common.query.PageQuery;
import product.vo.SpuInfoPageQuery;
/**
 * 商品（spu）服务：维护 spu 主表，并编排上架、下架与级联删除。
 *
 * <p>写操作的副作用不限于本地库：上架要跨服务写 Elasticsearch，删除要跨服务清 coupon 的优惠数据
 * 与 MinIO 里的图片，详见各方法说明。
 */
public interface SpuInfoService extends IService<SpuInfoEntity> {

    /**
     * 分页查询全部 spu，不带任何筛选条件。
     *
     * @param query 分页参数，{@code page} 为页码、{@code limit} 为每页条数，不能为 {@code null}
     * @return 分页结果，{@code rows} 为 spu 列表，品牌名与分类名不会回填
     */
    PageVO<SpuInfoEntity> queryPage(PageQuery query);

    /**
     * 新增商品：一次写入 spu、描述、图集、规格参数、sku、sku 图集与 sku 销售属性。
     *
     * <p>本地写入在一个事务里，但 coupon 的远程写入不可回滚：积分信息保存失败会抛异常回滚本地数据，
     * sku 优惠信息保存失败只记日志、不回滚。
     *
     * @param spuInfo 商品内容，含描述图、图集、规格参数与 sku 列表，不能为 {@code null}
     * @throws RuntimeException 积分信息保存失败时抛出，本地写入整批回滚
     */
    void saveSpuInfo(SpuVO spuInfo);

    /**
     * 按条件分页查询 spu，并回填品牌名与分类名。
     *
     * @param query 查询条件：{@code key} 匹配 spu ID 或 spu 名；{@code status} 为发布状态；
     *              {@code brandId}、{@code catalogId} 为空或 0 时不参与过滤；不能为 {@code null}
     * @return 分页结果，{@code rows} 为商品列表
     */
    PageVO<SpuInfoEntity> queryPageByCondition(SpuInfoPageQuery query);

    /**
     * 商品上架：把 spu 下的 sku 组装成 ES 文档推给 search，search 接受后才把发布状态置为上架。
     *
     * <p>库存服务异常时不阻断上架，该 spu 的 sku 一律按「有货」写入索引；search 返回非成功码时
     * 不改状态，也不抛异常。
     *
     * @param spuId spu ID，不能为 {@code null}
     */
    void up(Long spuId);

    /**
     * 下架 spu：本地只改状态并落一条 {@code product.down} 事件，ES 里的文档由 search 异步清掉。
     *
     * <p>幂等：已下架的商品也会照常重发事件，用于修掉「库说下架、搜索还能搜到」。
     *
     * @param spuId spu ID，不能为 {@code null}
     * @throws common.exception.ValidationException spu 不存在时抛出
     */
    void down(Long spuId);

    /**
     * 按 sku ID 反查所属 spu，并回填品牌名。
     *
     * @param skuId sku ID，不能为 {@code null}
     * @return 该 sku 所属的 spu；sku 不存在时会在取 spuId 处抛空指针异常，调用方需先确认 sku 有效
     */
    SpuInfoEntity getSpuInfoBySkuId(Long skuId);

    /**
     * 按 spu ID 批量取 spu 名称。
     *
     * @param list spu ID 列表，不能为 {@code null}
     * @return spuId 到 spu 名称的映射；入参里查不到的 ID 不会出现在结果中
     */
    Map<Long, String> getUserNames(List<Long> list);

    /**
     * 返回全部 spu 的 ID 与名称，供下拉框选择。
     *
     * <p>一次查出全表，不分页也不过滤。
     *
     * @return spu 选择项列表；没有 spu 时返回空列表
     */
    List<SpuSelectVO> getSpuSelect();

    /**
     * 级联删除 spu，连同其描述、图集、规格参数、sku、sku 图集与 sku 销售属性一起删。
     *
     * <p>本地 7 张表的删除与 {@code product.deleted} 事件在同一个事务内完成，coupon 的优惠数据与
     * MinIO 里的图片由消费方异步清理。已上架的商品（11001）、仓库侧还有库存或未完成采购需求的商品
     * 一律整批拒绝，入参为空同样拒绝；删除是幂等的，查不到的 ID 会被静默跳过。
     *
     * @param spuIds 待删除的 spu 主键列表，不能为 {@code null} 或空集合
     * @throws common.exception.ValidationException 入参为空时抛出
     * @throws common.exception.BaseException 商品已上架、仓库服务不可用或仓库仍有库存时抛出，删除整批不生效
     */
    void removeSpuInfo(List<Long> spuIds);

}


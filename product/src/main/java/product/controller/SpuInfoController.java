package product.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import common.vo.PageVO;
import common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import product.entity.SpuInfoEntity;
import product.service.SpuInfoService;
import product.vo.SpuSelectVO;
import product.vo.SpuVO;

import java.util.Arrays;
import java.util.List;
import java.util.Map;


import product.vo.SpuInfoPageQuery;
/**
 * 商品（spu）后台管理接口：下拉选择数据、按 sku 反查 spu、上下架，以及商品的分页列表、详情、新增、修改、删除。
 *
 * <p>路径不在 {@code /front} 下，经网关访问时按管理端接口鉴权。
 */
@RestController
@RequestMapping("product/spuinfo")
@Slf4j
public class SpuInfoController {
    private final SpuInfoService spuInfoService;

    /**
     * 由 Spring 注入 spu 服务，创建后即可直接调用。
     *
     * @param spuInfoService spu 服务
     */
    public SpuInfoController(SpuInfoService spuInfoService) {
        this.spuInfoService = spuInfoService;
    }

    /**
     * 返回全部 spu 的 ID 与名称，供下拉框选择。
     *
     * <p>一次查出全表，不分页也不过滤。
     *
     * @return spu 选择项列表，{@code id} 为 spu ID、{@code name} 为 spu 名称
     */
    @GetMapping(value = "/getSpuSelect")
    public R<List<SpuSelectVO>> getSpuSelect() {
        log.info("获取spu下拉框选择信息");
        List<SpuSelectVO> spuSelect = spuInfoService.getSpuSelect();

        return R.ok(spuSelect);
    }

    /**
     * 按 spu ID 批量取 spu 名称。
     *
     * @param spuIds spu ID 列表
     * @return spuId 到 spu 名称的映射；入参里查不到的 ID 不会出现在结果中
     */
    @PostMapping(value = "/getSpuNames")
    public R<Map<Long, String>> getSpuNames(@RequestBody List<Long> spuIds) {
        log.info("批量获取spuName：{}", JSON.toJSONString(spuIds, SerializerFeature.PrettyFormat));

        Map<Long, String> map = spuInfoService.getUserNames(spuIds);

        return R.ok(map);
    }

    /**
     * 按 sku ID 反查所属商品。
     *
     * <p>返回的 spu 已回填品牌名与主图地址；sku 不存在时会在取 spuId 处抛空指针异常。
     *
     * @param skuId sku ID
     * @return 该 sku 所属的 spu
     */
    @GetMapping(value = "/skuId/{skuId}")
    public R<SpuInfoEntity> getSpuInfoBySkuId(@PathVariable("skuId") Long skuId) {
        log.info("根据skuId查询spu信息");

        SpuInfoEntity spuInfoEntity = spuInfoService.getSpuInfoBySkuId(skuId);

        return R.ok(spuInfoEntity);
    }

    /**
     * 商品上架：把 spu 下的 sku 组装成 ES 文档推给 search，search 接受后才把发布状态置为上架。
     *
     * <p>库存服务异常时不阻断上架，该 spu 的 sku 一律按「有货」写入索引。
     *
     * @param spuId spu ID
     * @return 统一成功响应，不含业务数据
     */
    @PostMapping("/{spuId}/up")
    public R<Void> up(@PathVariable("spuId") Long spuId) {
        log.info("商品上架：{}", spuId);
        spuInfoService.up(spuId);

        return R.ok();
    }

    /**
     * 商品下架：改本地发布状态并落一条 {@code product.delisted} 事件，ES 文档由 search 异步清掉。
     *
     * <p>幂等：已下架的商品也会照常重发事件，用于修掉「库说下架、搜索还能搜到」。
     *
     * @param spuId spu ID
     * @return 统一成功响应，不含业务数据
     */
    @PostMapping("/{spuId}/down")
    public R<Void> down(@PathVariable("spuId") Long spuId) {
        log.info("商品下架：{}", spuId);
        spuInfoService.down(spuId);

        return R.ok();
    }

    /**
     * 按条件分页查询商品，并回填品牌名与分类名。
     *
     * @param query 查询条件：{@code key} 匹配 spu ID 或 spu 名；{@code status} 为发布状态；
     *              {@code brandId}、{@code catalogId} 为空或 0 时不参与过滤
     * @return 分页结果，{@code rows} 为商品列表
     */
    @RequestMapping("/list")
    public R<PageVO<SpuInfoEntity>> list(SpuInfoPageQuery query){
        log.info("列表查询spu：{}", JSON.toJSONString( query, SerializerFeature.PrettyFormat));
        PageVO<SpuInfoEntity> page = spuInfoService.queryPageByCondition(query);

        return R.ok(page);
    }


    /**
     * 按主键查询商品详情。
     *
     * @param id spu ID
     * @return 商品详情；不存在时 {@code data} 为 {@code null}
     */
    @RequestMapping("/info/{id}")
    public R<SpuInfoEntity> info(@PathVariable("id") Long id){
        log.info("根据id查询spu：{}", id);
		SpuInfoEntity spuInfo = spuInfoService.getById(id);

        return R.ok(spuInfo);
    }

    /**
     * 新增商品：一次写入 spu、描述、图集、规格参数、sku、sku 图集与 sku 销售属性，并远程保存积分与满减规则。
     *
     * <p>本地写入在一个事务里，但 coupon 的远程写入不可回滚：积分信息保存失败会抛异常回滚本地数据，
     * sku 优惠信息保存失败只记日志、不回滚。
     *
     * @param spuInfo 商品内容，含描述图、图集、规格参数与 sku 列表
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody SpuVO spuInfo){
        log.info("保存spu：{}", JSON.toJSONString(spuInfo, SerializerFeature.PrettyFormat));
		spuInfoService.saveSpuInfo(spuInfo);

        return R.ok();
    }

    /**
     * 按主键修改商品主表。
     *
     * <p>描述、图集、规格参数与 sku 都不在本接口的修改范围内。
     *
     * @param spuInfo 商品内容，主键必填；为 {@code null} 的字段不参与更新
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody SpuInfoEntity spuInfo){
        log.info("修改spu：{}", JSON.toJSONString(spuInfo, SerializerFeature.PrettyFormat));
		spuInfoService.updateById(spuInfo);

        return R.ok();
    }

    /**
     * 级联删除商品。
     *
     * <p>同步删掉商品自己的 7 张表，并落一条 {@code product.deleted} 消息；coupon 里的积分/满减/打折/会员价
     * 和 MinIO 里的图片由消费方异步清掉（本地消息表 + 定时重投保证最终一定会清）。
     *
     * <p>已上架的商品需要先下架，仓库侧还有库存或在途采购的商品需要先处理，两种情况都整批拒绝；
     * {@code ids} 为空时也整批拒绝。
     *
     * @param ids 待删除的 spu 主键数组，不能为空
     * @return 统一成功响应，不含业务数据
     */
    @PostMapping("/delete")
    public R<Void> delete(@RequestBody Long[] ids){
        log.info("删除spu：{}", JSON.toJSONString(ids, SerializerFeature.PrettyFormat));
		spuInfoService.removeSpuInfo(ids == null ? List.of() : Arrays.asList(ids));

        return R.ok();
    }

}

package product.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import common.to.SkuScopeVo;
import common.vo.PageVO;
import common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import product.entity.SkuInfoEntity;
import product.service.SkuInfoService;
import product.vo.SkuSelectVO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;


import product.vo.SkuInfoPageQuery;
/**
 * sku 后台管理接口：下拉选择数据、按 sku 批量取名称与价格，以及 sku 的分页列表、详情、新增、修改。
 *
 * <p>路径不在 {@code /front} 下，经网关访问时按管理端接口鉴权。
 *
 * <p>没有删除接口：sku 的生命周期由 spu 管理，按主键直接删会绕过
 * {@code SpuInfoServiceImpl#removeSpuInfo} 的子表清理、仓库库存与在途采购校验，删除商品请走
 * {@code /product/spuinfo/delete}。
 */
@RestController
@RequestMapping("product/skuinfo")
@Slf4j
public class SkuInfoController {
    private final SkuInfoService skuInfoService;

    /**
     * 由 Spring 注入 sku 服务，创建后即可直接调用。
     *
     * @param skuInfoService sku 服务
     */
    public SkuInfoController(SkuInfoService skuInfoService) {
        this.skuInfoService = skuInfoService;
    }

    /**
     * 返回全部 sku 的 ID 与名称，供下拉框选择。
     *
     * <p>一次查出全表，不分页也不过滤。
     *
     * @return sku 选择项列表，{@code id} 为 sku ID、{@code name} 为 sku 名称
     */
    @GetMapping(value = "/getSkuSelect")
    public R<List<SkuSelectVO>> getSkuSelect() {
        log.info("获取sku下拉框选择信息");
        List<SkuSelectVO> skuSelect = skuInfoService.getSkuSelect();

        return R.ok(skuSelect);
    }


    /**
     * 按 sku ID 批量取 sku 名称。
     *
     * <p>参数名是 {@code spuIds}，实际按 sku 主键查询，返回的 Map 也以 skuId 为键。
     *
     * @param spuIds sku ID 列表
     * @return skuId 到 sku 名称的映射；入参里查不到的 ID 不会出现在结果中
     */
    @PostMapping(value = "/getSkuNames")
    public R<Map<Long, String>> getSkuNames(@RequestBody List<Long> spuIds) {
        log.info("批量获取spuName：{}", JSON.toJSONString(spuIds, SerializerFeature.PrettyFormat));

        Map<Long, String> map = skuInfoService.getUserNames(spuIds);

        return R.ok(map);
    }

    /**
     * 按 sku ID 批量取它在商品层级里的归属：所属 SPU 与分类。
     *
     * <p>给下游做"指定商品 / 指定分类"的规则匹配用（优惠券的适用范围就是这种规则）。
     * 入参为空集合时返回空映射而不是报错。
     *
     * @param skuIds sku ID 列表
     * @return skuId 到归属信息的映射；入参里查不到的 ID 不会出现在结果中
     */
    @PostMapping(value = "/getSkuScopes")
    public R<Map<Long, SkuScopeVo>> getSkuScopes(@RequestBody List<Long> skuIds) {
        return R.ok(skuInfoService.getSkuScopes(skuIds));
    }

    /**
     * 查询指定 sku 的价格。
     *
     * <p>直接返回价格数值，不套 {@code R} 外壳；sku 不存在时取价处会抛空指针异常，由全局异常
     * 处理器兜成 {@code 10000}（未知异常），而不是 {@code 11008}。
     *
     * @param skuId sku ID
     * @return sku 价格，单位：元
     */
    @GetMapping(value = "/{skuId}/price")
    public BigDecimal getPrice(@PathVariable("skuId") Long skuId) {

        SkuInfoEntity skuInfo = skuInfoService.getById(skuId);

        BigDecimal price = skuInfo.getPrice();

        return price;
    }

    /**
     * 按条件分页查询 sku。
     *
     * @param query 查询条件：{@code key} 匹配 sku ID 或 sku 名；{@code catalogId}、{@code brandId}
     *              为空或 0 时不参与过滤；{@code min} 与 {@code max} 同时给出且 {@code min < max}
     *              时才按价格区间过滤
     * @return 分页结果，{@code rows} 为 sku 列表
     */
    @RequestMapping("/list")
    public R<PageVO<SkuInfoEntity>> list(SkuInfoPageQuery query){
        PageVO<SkuInfoEntity> page = skuInfoService.queryPageByCondition(query);

        return R.ok(page);
    }


    /**
     * 按主键查询 sku 详情。
     *
     * @param skuId sku ID
     * @return sku 详情；不存在时 {@code data} 为 {@code null}
     */
    @RequestMapping("/info/{skuId}")
    public R<SkuInfoEntity> info(@PathVariable("skuId") Long skuId){
		SkuInfoEntity skuInfo = skuInfoService.getById(skuId);

        return R.ok(skuInfo);
    }

    /**
     * 新增 sku。
     *
     * <p>只写 sku 主表，不写图集与销售属性；商品维度的完整新增走 {@code /product/spuinfo/save}。
     *
     * @param skuInfo sku 内容，主键由数据库生成
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody SkuInfoEntity skuInfo){
		skuInfoService.save(skuInfo);

        return R.ok();
    }

    /**
     * 按主键修改 sku。
     *
     * @param skuInfo sku 内容，主键必填；为 {@code null} 的字段不参与更新
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody SkuInfoEntity skuInfo){
		skuInfoService.updateById(skuInfo);

        return R.ok();
    }

}

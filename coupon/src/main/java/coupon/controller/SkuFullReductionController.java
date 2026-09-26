package coupon.controller;

import common.to.SkuReductionTo;
import common.vo.PageVO;
import common.utils.R;
import coupon.entity.SkuFullReductionEntity;
import coupon.service.SkuFullReductionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;



import common.query.KeyPageQuery;
/**
 * 商品满减的后台管理接口：分页列表、详情、新增、修改、删除。
 *
 * <p>另有一个供 product 发布商品时调用的优惠信息写入接口，会一并写阶梯价与会员价。
 */
@RestController
@RequestMapping("coupon/skufullreduction")
public class SkuFullReductionController {
    @Autowired
    private SkuFullReductionService skuFullReductionService;

    /**
     * 保存 SKU 的优惠信息，一次写入阶梯价、满减与会员价三张表。
     *
     * <p>由 product 发布商品时经 Feign 调用，三段数据各自判断：满件数大于 0 才写阶梯价，
     * 满金额大于 0 才写满减，会员价列表非空且价格大于 0 才写会员价；整批在一个事务内。
     *
     * @param skuReductionTo SKU 优惠信息，{@code skuId} 必填
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/saveInfo")
    public R<Void> saveInfo(@RequestBody SkuReductionTo skuReductionTo){
        skuFullReductionService.saveSkuReduction(skuReductionTo);

        return R.ok();
    }

    /**
     * 分页查询商品满减。
     *
     * <p>{@code key} 的过滤在内存中完成：先按分页取数，再用 Feign 回填的 SKU 名称筛，
     * 因此 {@code total} 是过滤前的总行数，{@code rows} 可能少于 {@code limit}。
     *
     * @param query 分页参数，{@code key} 全等匹配记录 ID 或模糊匹配 SKU 名称，{@code page} 为页码、{@code limit} 为每页条数
     * @return 分页结果，{@code rows} 为满减列表，SKU 名称由 product 服务回填
     */
    @RequestMapping("/list")
    public R<PageVO<SkuFullReductionEntity>> list(KeyPageQuery query){
        PageVO<SkuFullReductionEntity> page = skuFullReductionService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 按主键查询单条商品满减记录。
     *
     * @param id 满减记录主键
     * @return 满减详情；id 不存在时 {@code data} 为 {@code null}
     */
    @RequestMapping("/info/{id}")
    public R<SkuFullReductionEntity> info(@PathVariable("id") Long id){
		SkuFullReductionEntity skuFullReduction = skuFullReductionService.getById(id);

        return R.ok(skuFullReduction);
    }

    /**
     * 新增一条商品满减记录。
     *
     * @param skuFullReduction 满减内容，主键留空时由数据库生成
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody SkuFullReductionEntity skuFullReduction){
		skuFullReductionService.save(skuFullReduction);

        return R.ok();
    }

    /**
     * 按主键修改一条商品满减记录。
     *
     * @param skuFullReduction 满减内容，主键必填；为 {@code null} 的字段不参与更新
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody SkuFullReductionEntity skuFullReduction){
		skuFullReductionService.updateById(skuFullReduction);

        return R.ok();
    }

    /**
     * 按主键批量删除商品满减记录。
     *
     * @param ids 待删除的满减记录主键数组
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] ids){
		skuFullReductionService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}

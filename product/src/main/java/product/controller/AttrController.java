package product.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import common.vo.PageVO;
import common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import product.entity.AttrEntity;
import product.entity.ProductAttrValueEntity;
import product.service.AttrService;
import product.service.ProductAttrValueService;
import product.vo.AttrRespVO;
import product.vo.AttrVO;

import java.util.Arrays;
import java.util.List;
import java.util.Map;


import common.query.PageQuery;
import common.query.KeyPageQuery;
/**
 * 商品属性后台管理接口：按分类与属性类型分页查询属性、查详情、增删改，以及商品规格参数的查询与全量覆盖。
 *
 * <p>路径不在 {@code /front} 下，经网关访问时按管理端接口鉴权。
 */
@RestController
@RequestMapping("product/attr")
@Slf4j
public class AttrController {
    private final AttrService attrService;

    private final ProductAttrValueService productAttrValueService;

    /**
     * 由 Spring 注入属性与规格参数值服务，创建后即可直接调用。
     *
     * @param attrService 属性服务
     * @param productAttrValueService 商品规格参数值服务
     */
    public AttrController(AttrService attrService, ProductAttrValueService productAttrValueService) {
        this.attrService = attrService;
        this.productAttrValueService = productAttrValueService;
    }


    /**
     * 查询指定商品（spu）的规格参数及其值。
     *
     * @param spuId spu ID
     * @return 该 spu 的全部规格参数值；未维护过规格参数时返回空列表
     */
    @GetMapping("/base/listforspu/{spuId}")
    public R<List<ProductAttrValueEntity>> baseAttrListForSpu(@PathVariable Long spuId) {
        log.info("通过spuId查询商品规格属性：{}", spuId);
        List<ProductAttrValueEntity> productAttrValueEntities = productAttrValueService.baseAttrListForSpu(spuId);
        return R.ok(productAttrValueEntities);
    }

    /**
     * 分页查询某分类下的属性，并补全所属分组名与分类名。
     *
     * <p>{@code attrType} 只区分 {@code base} 与其余取值：传 {@code base} 查基本属性，传其他任何
     * 值（含拼错的值）一律按销售属性查询，不会报错。
     *
     * @param query 分页与关键字条件，{@code key} 同时匹配属性 ID 与属性名
     * @param category 三级分类 ID，为 {@code null} 或 0 时不按分类过滤
     * @param attrType 属性类型，{@code base} 为基本属性，其余为销售属性
     * @return 分页结果，每行含分组名、分组 ID 与分类名
     */
    @RequestMapping("/{attrType}/list/{category}")
    public R<PageVO<AttrRespVO>> baseAttrList(KeyPageQuery query, @PathVariable Long category, @PathVariable String attrType) {
        log.info("查询商品属性：{}--{}--{}", JSON.toJSONString(query, SerializerFeature.PrettyFormat), category, attrType);
        PageVO<AttrRespVO> page = attrService.queryBaseAttrPage(query, category, attrType);

        return R.ok(page);
    }

    /**
     * 分页查询全部商品属性，不带筛选条件。
     *
     * @param query 分页参数，{@code page} 为页码、{@code limit} 为每页条数
     * @return 分页结果，{@code rows} 为属性列表
     */
    @RequestMapping("/list")
    public R<PageVO<AttrEntity>> list(PageQuery query) {
        log.info("查询商品属性：{}", JSON.toJSONString(query, SerializerFeature.PrettyFormat));
        PageVO<AttrEntity> page = attrService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 按主键查询属性详情。
     *
     * @param attrId 属性 ID
     * @return 属性详情，含所属分组名与分组 ID、分类名，以及从一级分类到本级分类的路径
     */
    @RequestMapping("/info/{attrId}")
    public R<AttrRespVO> info(@PathVariable("attrId") Long attrId) {
        log.info("根据id查询商品属性：{}", attrId);
        AttrRespVO attr = attrService.getAttrInfo(attrId);

        return R.ok(attr);
    }

    /**
     * 新增商品属性。
     *
     * <p>基本属性额外写一条属性与属性分组的关联行，销售属性不写。
     *
     * @param attr 属性内容，基本属性需带 {@code attrGroupId}
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody AttrVO attr) {
        log.info("保存商品属性：{}", attr);
        attrService.saveAttr(attr);

        return R.ok();
    }

    /**
     * 修改商品属性。
     *
     * <p>图标被换掉时先让 third-party 删除旧图标对象，删除失败则整笔回滚；基本属性会顺带新增
     * 或更新属性与属性分组的关联，销售属性不动关联。
     *
     * @param attr 属性内容，主键必填
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody AttrVO attr) {
        log.info("修改商品属性：{}", attr);
        attrService.updateAttr(attr);

        return R.ok();
    }

    /**
     * 全量覆盖指定商品的规格参数。
     *
     * <p>先按 {@code spuId} 删光原有规格参数再插入入参，入参里的 {@code spuId} 会被忽略并统一
     * 改写为路径上的值；已上架的商品不允许改规格参数，会整批拒绝。
     *
     * @param entities 新的规格参数列表
     * @param spuId spu ID，商品不存在或已上架时不执行覆盖
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/update/{spuId}")
    public R<Void> update(@RequestBody List<ProductAttrValueEntity> entities, @PathVariable Long spuId) {
        log.info("修改商品规格：{}--{}", entities, spuId);
        productAttrValueService.updateSpuAttr(spuId, entities);
        return R.ok();
    }

    /**
     * 按主键批量删除商品属性。
     *
     * <p>属性还被商品规格参数或 sku 销售属性引用时整批拒绝；属性与属性分组的关联行跟着一起删，
     * 属性图标对象也会同步删掉。传不存在的 ID 会被静默跳过。
     *
     * @param attrIds 待删除的属性主键数组
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] attrIds) {
        log.info("删除商品属性：{}", JSON.toJSONString(attrIds, SerializerFeature.PrettyFormat));
        attrService.deleteByIds(Arrays.asList(attrIds));

        return R.ok();
    }

}

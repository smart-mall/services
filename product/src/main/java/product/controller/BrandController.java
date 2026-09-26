package product.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import common.vo.PageVO;
import common.utils.R;
import common.valid.AddGroup;
import common.valid.UpdateGroup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import product.entity.BrandEntity;
import product.service.BrandService;

import java.util.Arrays;
import java.util.Map;


import common.query.KeyPageQuery;
/**
 * 品牌后台管理接口：分页列表、详情、新增、修改、删除。
 *
 * <p>路径不在 {@code /front} 下，经网关访问时按管理端接口鉴权；品牌与分类的关联维护走
 * {@link CategoryBrandRelationController}。
 */
@RestController
@RequestMapping("product/brand")
@Slf4j
public class BrandController {
    @Autowired
    private BrandService brandService;

    /**
     * 按关键字分页查询品牌。
     *
     * @param query 分页与关键字条件，{@code key} 同时模糊匹配品牌名与品牌 ID
     * @return 分页结果，{@code rows} 为品牌列表
     */
    @RequestMapping("/list")
    public R<PageVO<BrandEntity>> list(KeyPageQuery query){
        log.info("显示品牌：{}", JSON.toJSONString( query, SerializerFeature.PrettyFormat));
        PageVO<BrandEntity> page = brandService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 按主键查询品牌详情。
     *
     * @param brandId 品牌 ID
     * @return 品牌详情；不存在时 {@code data} 为 {@code null}
     */
    @RequestMapping("/info/{brandId}")
    public R<BrandEntity> info(@PathVariable("brandId") Long brandId){
        log.info("获取品牌：{}", brandId);
		BrandEntity brand = brandService.getById(brandId);

        return R.ok(brand);
    }

    /**
     * 新增品牌。
     *
     * <p>按 {@code AddGroup} 分组校验：品牌 ID 必须为空，品牌名、logo、介绍、显示状态、检索首字母
     * 与排序都不能缺，校验失败返回字段级错误明细。
     *
     * @param brand 品牌内容，主键由数据库生成
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/save")
    public R<Void> save(@Validated(AddGroup.class) @RequestBody BrandEntity brand){
        log.info("保存品牌：{}", JSON.toJSONString(brand, SerializerFeature.PrettyFormat));

		brandService.save(brand);

        return R.ok();
    }

    /**
     * 修改品牌。
     *
     * <p>logo 被换掉时先让 third-party 删除旧 logo 对象，删除失败则整笔回滚；同时同步更新品牌
     * 分类关联表里冗余的品牌名。
     *
     * @param brand 品牌内容，{@code brandId} 必填，按 {@code UpdateGroup} 分组校验
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/update")
    public R<Void> update(@Validated(UpdateGroup.class) @RequestBody BrandEntity brand){
        log.info("修改品牌：{}", JSON.toJSONString(brand, SerializerFeature.PrettyFormat));
		brandService.updateDetail(brand);

        return R.ok();
    }

    /**
     * 按主键批量删除品牌。
     *
     * <p>品牌下还有商品时整批拒绝；品牌与分类的关联行跟着一起删，品牌 logo 对象也会同步删掉。
     *
     * @param brandIds 待删除的品牌主键数组
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] brandIds){
        log.info("删除品牌：{}", JSON.toJSONString(brandIds, SerializerFeature.PrettyFormat));
		brandService.deleteByIds(Arrays.asList(brandIds));

        return R.ok();
    }

}

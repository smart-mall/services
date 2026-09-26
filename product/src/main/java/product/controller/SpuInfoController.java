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
 * spu信息
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 09:58:33
 */
@RestController
@RequestMapping("product/spuinfo")
@Slf4j
public class SpuInfoController {
    private final SpuInfoService spuInfoService;

    public SpuInfoController(SpuInfoService spuInfoService) {
        this.spuInfoService = spuInfoService;
    }

    // 获取spu下拉框选择信息
    @GetMapping(value = "/getSpuSelect")
    public R<List<SpuSelectVO>> getSpuSelect() {
        log.info("获取spu下拉框选择信息");
        List<SpuSelectVO> spuSelect = spuInfoService.getSpuSelect();

        return R.ok(spuSelect);
    }

    // 批量获取spuName
    @PostMapping(value = "/getSpuNames")
    public R<Map<Long, String>> getSpuNames(@RequestBody List<Long> spuIds) {
        log.info("批量获取spuName：{}", JSON.toJSONString(spuIds, SerializerFeature.PrettyFormat));

        Map<Long, String> map = spuInfoService.getUserNames(spuIds);

        return R.ok(map);
    }

    @GetMapping(value = "/skuId/{skuId}")
    public R<SpuInfoEntity> getSpuInfoBySkuId(@PathVariable("skuId") Long skuId) {
        log.info("根据skuId查询spu信息");

        SpuInfoEntity spuInfoEntity = spuInfoService.getSpuInfoBySkuId(skuId);

        return R.ok(spuInfoEntity);
    }

    /**
     * 商品上架
     */
    @PostMapping("/{spuId}/up")
    public R<Void> up(@PathVariable("spuId") Long spuId) {
        log.info("商品上架：{}", spuId);
        spuInfoService.up(spuId);

        return R.ok();
    }

    /**
     * 商品下架
     */
    @PostMapping("/{spuId}/down")
    public R<Void> down(@PathVariable("spuId") Long spuId) {
        log.info("商品下架：{}", spuId);
        spuInfoService.down(spuId);

        return R.ok();
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R<PageVO<SpuInfoEntity>> list(SpuInfoPageQuery query){
        log.info("列表查询spu：{}", JSON.toJSONString( query, SerializerFeature.PrettyFormat));
        PageVO<SpuInfoEntity> page = spuInfoService.queryPageByCondition(query);

        return R.ok(page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R<SpuInfoEntity> info(@PathVariable("id") Long id){
        log.info("根据id查询spu：{}", id);
		SpuInfoEntity spuInfo = spuInfoService.getById(id);

        return R.ok(spuInfo);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody SpuVO spuInfo){
        log.info("保存spu：{}", JSON.toJSONString(spuInfo, SerializerFeature.PrettyFormat));
		spuInfoService.saveSpuInfo(spuInfo);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody SpuInfoEntity spuInfo){
        log.info("修改spu：{}", JSON.toJSONString(spuInfo, SerializerFeature.PrettyFormat));
		spuInfoService.updateById(spuInfo);

        return R.ok();
    }

    /**
     * 删除。
     *
     * <p>同步删掉的是商品自己的 7 张表，并落一条 {@code product.deleted} 消息；
     * coupon 里的积分/满减/打折/会员价和 MinIO 里的图片由消费方异步清掉
     * （本地消息表 + 定时重投保证最终一定会清）。已上架的商品不允许删除，需要先下架。</p>
     */
    @PostMapping("/delete")
    public R<Void> delete(@RequestBody Long[] ids){
        log.info("删除spu：{}", JSON.toJSONString(ids, SerializerFeature.PrettyFormat));
		spuInfoService.removeSpuInfo(ids == null ? List.of() : Arrays.asList(ids));

        return R.ok();
    }

}

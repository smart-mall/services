package coupon.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import common.vo.PageVO;
import common.utils.R;
import coupon.entity.SpuBoundsEntity;
import coupon.service.SpuBoundsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;



import common.query.KeyPageQuery;
/**
 * 商品spu积分设置
 */
@RestController
@RequestMapping("coupon/spubounds")
@Slf4j
public class SpuBoundsController {
    private final SpuBoundsService spuBoundsService;

    public SpuBoundsController(SpuBoundsService spuBoundsService) {
        this.spuBoundsService = spuBoundsService;
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R<PageVO<SpuBoundsEntity>> list(KeyPageQuery query){
        log.info("列表查询spuBounds：{}", JSON.toJSONString( query, SerializerFeature.PrettyFormat));
        PageVO<SpuBoundsEntity> page = spuBoundsService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R<SpuBoundsEntity> info(@PathVariable("id") Long id){
		SpuBoundsEntity spuBounds = spuBoundsService.getById(id);
        log.info("根据id查询spuBounds：{}", id);

        return R.ok(spuBounds);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody SpuBoundsEntity spuBounds){
        log.info("保存spuBounds：{}", JSON.toJSONString(spuBounds, SerializerFeature.PrettyFormat));
		spuBoundsService.save(spuBounds);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody SpuBoundsEntity spuBounds){
        log.info("修改spuBounds：{}", JSON.toJSONString(spuBounds, SerializerFeature.PrettyFormat));
		spuBoundsService.updateById(spuBounds);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] ids){
        log.info("删除spuBounds：{}", JSON.toJSONString(ids, SerializerFeature.PrettyFormat));
		spuBoundsService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}

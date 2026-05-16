package coupon.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import common.utils.PageUtils;
import common.utils.R;
import coupon.entity.SeckillPromotionEntity;
import coupon.service.SeckillPromotionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;



/**
 * 秒杀活动
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 11:10:43
 */
@RestController
@RequestMapping("coupon/seckillpromotion")
@Slf4j
public class SeckillPromotionController {
    private final SeckillPromotionService seckillPromotionService;

    public SeckillPromotionController(SeckillPromotionService seckillPromotionService) {
        this.seckillPromotionService = seckillPromotionService;
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params){
        log.info("列表：{}", JSON.toJSONString(params, SerializerFeature.PrettyFormat));
        PageUtils page = seckillPromotionService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id){
        log.info("通过id查询：{}", id);
		SeckillPromotionEntity seckillPromotion = seckillPromotionService.getById(id);

        return R.ok().put("seckillPromotion", seckillPromotion);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody SeckillPromotionEntity seckillPromotion){
        log.info("保存：{}", JSON.toJSONString(seckillPromotion, SerializerFeature.PrettyFormat));
        seckillPromotion.setCreateTime(new Date());
        seckillPromotionService.save(seckillPromotion);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody SeckillPromotionEntity seckillPromotion){
        log.info("修改：{}", JSON.toJSONString(seckillPromotion, SerializerFeature.PrettyFormat));
		seckillPromotionService.updateById(seckillPromotion);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] ids){
        log.info("删除：{}", JSON.toJSONString(ids, SerializerFeature.PrettyFormat));
		seckillPromotionService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}

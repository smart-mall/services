package coupon.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import common.vo.PageVO;
import common.utils.R;
import coupon.entity.SeckillPromotionEntity;
import coupon.service.SeckillPromotionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;



import common.query.KeyPageQuery;
/**
 * 秒杀活动的后台管理接口：分页列表、详情、新增、修改、删除。
 *
 * <p>路径不在 {@code /front} 下，经网关访问时按管理端接口鉴权。
 */
@RestController
@RequestMapping("coupon/seckillpromotion")
@Slf4j
public class SeckillPromotionController {
    private final SeckillPromotionService seckillPromotionService;

    /**
     * 创建秒杀活动管理接口实例。
     *
     * @param seckillPromotionService 秒杀活动业务服务，由容器注入
     */
    public SeckillPromotionController(SeckillPromotionService seckillPromotionService) {
        this.seckillPromotionService = seckillPromotionService;
    }

    /**
     * 分页查询秒杀活动。
     *
     * @param query 分页参数，{@code key} 模糊匹配活动标题或活动 ID，{@code page} 为页码、{@code limit} 为每页条数
     * @return 分页结果，{@code rows} 为秒杀活动列表，创建人名称由 renren-fast 回填
     */
    @RequestMapping("/list")
    public R<PageVO<SeckillPromotionEntity>> list(KeyPageQuery query){
        log.info("列表：{}", JSON.toJSONString(query, SerializerFeature.PrettyFormat));
        PageVO<SeckillPromotionEntity> page = seckillPromotionService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 按主键查询单条秒杀活动。
     *
     * @param id 秒杀活动主键
     * @return 秒杀活动详情；id 不存在时 {@code data} 为 {@code null}
     */
    @RequestMapping("/info/{id}")
    public R<SeckillPromotionEntity> info(@PathVariable("id") Long id){
        log.info("通过id查询：{}", id);
		SeckillPromotionEntity seckillPromotion = seckillPromotionService.getById(id);

        return R.ok(seckillPromotion);
    }

    /**
     * 新增一条秒杀活动。
     *
     * <p>创建时间由本接口取当前时刻填充，请求体里传的值会被覆盖。
     *
     * @param seckillPromotion 秒杀活动内容，主键留空时由数据库生成
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody SeckillPromotionEntity seckillPromotion){
        log.info("保存：{}", JSON.toJSONString(seckillPromotion, SerializerFeature.PrettyFormat));
        seckillPromotion.setCreateTime(new Date());
        seckillPromotionService.save(seckillPromotion);

        return R.ok();
    }

    /**
     * 按主键修改一条秒杀活动。
     *
     * @param seckillPromotion 秒杀活动内容，主键必填；为 {@code null} 的字段不参与更新
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody SeckillPromotionEntity seckillPromotion){
        log.info("修改：{}", JSON.toJSONString(seckillPromotion, SerializerFeature.PrettyFormat));
		seckillPromotionService.updateById(seckillPromotion);

        return R.ok();
    }

    /**
     * 按主键批量删除秒杀活动。
     *
     * @param ids 待删除的秒杀活动主键数组
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] ids){
        log.info("删除：{}", JSON.toJSONString(ids, SerializerFeature.PrettyFormat));
		seckillPromotionService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}

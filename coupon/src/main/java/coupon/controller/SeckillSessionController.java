package coupon.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import common.utils.PageUtils;
import common.utils.R;
import coupon.entity.SeckillSessionEntity;
import coupon.service.SeckillSessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;


/**
 * 秒杀活动场次
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 11:10:43
 */
@RestController
@RequestMapping("coupon/seckillsession")
@Slf4j
public class SeckillSessionController {
    @Autowired
    private SeckillSessionService seckillSessionService;

    /**
     * 查询最近三天需要参加秒杀商品的信息
     *
     * @return
     */
    @GetMapping(value = "/Lates3DaySession")
    public R getLates3DaySession() {
        log.info("查询最近三天需要参加秒杀商品信息");
        List<SeckillSessionEntity> seckillSessionEntities = seckillSessionService.getLates3DaySession();

        return R.ok().setData(seckillSessionEntities);
    }


    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params) {
        log.info("列表查询：{}", JSON.toJSONString(params, SerializerFeature.PrettyFormat));
        PageUtils page = seckillSessionService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id) {
        log.info("信息查询：{}", id);
        SeckillSessionEntity seckillSession = seckillSessionService.getById(id);

        return R.ok().put("seckillSession", seckillSession);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody SeckillSessionEntity seckillSession) {
        seckillSession.setCreateTime(new Date());
        log.info("保存：{}", JSON.toJSONString(seckillSession, SerializerFeature.PrettyFormat));
        seckillSessionService.save(seckillSession);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody SeckillSessionEntity seckillSession) {
        log.info("修改：{}", JSON.toJSONString(seckillSession, SerializerFeature.PrettyFormat));
        seckillSessionService.updateById(seckillSession);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] ids) {
        log.info("删除：{}", JSON.toJSONString(ids, SerializerFeature.PrettyFormat));
        seckillSessionService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}

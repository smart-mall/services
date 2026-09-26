package coupon.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import common.vo.PageVO;
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


import common.query.KeyPageQuery;
/**
 * 秒杀活动场次的后台管理接口：分页列表、详情、新增、修改、删除。
 *
 * <p>另有一个供 seckill 服务定时上架调用的近三天场次查询，走 Feign 直连，不经网关鉴权。
 */
@RestController
@RequestMapping("coupon/seckillsession")
@Slf4j
public class SeckillSessionController {
    @Autowired
    private SeckillSessionService seckillSessionService;

    /**
     * 查询最近三天需要上架的秒杀场次，并装配每个场次下参与秒杀的商品关联。
     *
     * <p>由 seckill 服务的定时上架任务经 Feign 调用，时间范围是今天 00:00:00 至后天 23:59:59。
     *
     * @return 场次列表；这三天内没有场次时 {@code data} 为 {@code null} 而非空集合，调用方需判空
     */
    @GetMapping(value = "/Lates3DaySession")
    public R<List<SeckillSessionEntity>> getLates3DaySession() {
        log.info("查询最近三天需要参加秒杀商品信息");
        List<SeckillSessionEntity> seckillSessionEntities = seckillSessionService.getLates3DaySession();

        return R.ok(seckillSessionEntities);
    }


    /**
     * 分页查询秒杀活动场次。
     *
     * @param query 分页参数，{@code key} 模糊匹配场次名称并全等匹配 ID，{@code page} 为页码、{@code limit} 为每页条数
     * @return 分页结果，{@code rows} 为场次列表
     */
    @RequestMapping("/list")
    public R<PageVO<SeckillSessionEntity>> list(KeyPageQuery query) {
        log.info("列表查询：{}", JSON.toJSONString(query, SerializerFeature.PrettyFormat));
        PageVO<SeckillSessionEntity> page = seckillSessionService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 按主键查询单条秒杀活动场次。
     *
     * @param id 场次主键
     * @return 场次详情；id 不存在时 {@code data} 为 {@code null}
     */
    @RequestMapping("/info/{id}")
    public R<SeckillSessionEntity> info(@PathVariable("id") Long id) {
        log.info("信息查询：{}", id);
        SeckillSessionEntity seckillSession = seckillSessionService.getById(id);

        return R.ok(seckillSession);
    }

    /**
     * 新增一条秒杀活动场次。
     *
     * <p>创建时间由本接口取当前时刻填充，请求体里传的值会被覆盖。
     *
     * @param seckillSession 场次内容，主键留空时由数据库生成
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody SeckillSessionEntity seckillSession) {
        seckillSession.setCreateTime(new Date());
        log.info("保存：{}", JSON.toJSONString(seckillSession, SerializerFeature.PrettyFormat));
        seckillSessionService.save(seckillSession);

        return R.ok();
    }

    /**
     * 按主键修改一条秒杀活动场次。
     *
     * @param seckillSession 场次内容，主键必填；为 {@code null} 的字段不参与更新
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody SeckillSessionEntity seckillSession) {
        log.info("修改：{}", JSON.toJSONString(seckillSession, SerializerFeature.PrettyFormat));
        seckillSessionService.updateById(seckillSession);

        return R.ok();
    }

    /**
     * 按主键批量删除秒杀活动场次。
     *
     * @param ids 待删除的场次主键数组
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] ids) {
        log.info("删除：{}", JSON.toJSONString(ids, SerializerFeature.PrettyFormat));
        seckillSessionService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}

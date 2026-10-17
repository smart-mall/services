package seckill.controller;

import common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import seckill.service.SeckillService;
import seckill.vo.SeckillUploadResultVo;

/**
 * 秒杀管理端接口：提供后台手动触发上架。
 *
 * <p>路径落在 {@code /api/seckill/admin/**}，属于网关的管理端路径，调用方必须带管理端 token，
 * 由网关调 renren-fast 校验登录态后放行。本控制器自身不做权限判断，也不细分菜单权限。
 *
 * <p>无状态、线程安全。服务端口只在容器网络内暴露，外部只能经网关访问。
 */
@Slf4j
@RestController
@RequestMapping("seckill/admin")
public class SeckillAdminController {

    private final SeckillService seckillService;

    /**
     * @param seckillService 秒杀业务服务，由容器注入
     */
    public SeckillAdminController(SeckillService seckillService) {
        this.seckillService = seckillService;
    }

    /**
     * 手动触发一次秒杀上架。
     *
     * <p>与每天 0 点的定时任务走同一条流程、共用同一把分布式锁，因此两者不会同时上架。
     * 重复触发不会覆盖已上架的数据，只补齐缓存里还没有的场次与商品。
     *
     * @return 统一响应体，数据为本次的场次数、新写入与跳过的商品条目数
     */
    @PostMapping("/upload")
    public R<SeckillUploadResultVo> upload() {
        log.info("手动触发秒杀上架");
        return R.ok(seckillService.uploadSeckillSkuLatest3Days());
    }
}

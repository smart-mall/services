package seckill.feign;

import common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import seckill.vo.SeckillSessionWithSkusVo;
/**
 * coupon 服务的 Feign 客户端：拉取最近三天的秒杀场次，供定时上架任务使用。
 *
 * <p>方法由 Feign 动态代理实现，路径要与 coupon 侧保持一致，改动需两边同步。
 */
@FeignClient("coupon")
public interface CouponFeignService {

    /**
     * 查询最近三天需要参与的秒杀场次及其关联 SKU。
     *
     * @return 场次列表，包在统一响应体里；调用方需先判断 {@code code} 为 0 再取 {@code data}
     */
    @GetMapping(value = "/coupon/seckillsession/Lates3DaySession")
    R<List<SeckillSessionWithSkusVo>> getLates3DaySession();

}

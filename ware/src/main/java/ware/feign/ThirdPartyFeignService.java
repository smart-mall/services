package ware.feign;

import common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 第三方服务的 Feign 客户端：按行政区划编码查询直线距离，用于按距离计算运费。
 *
 * <p>跨服务契约，路径与参数名以第三方服务 {@code AddressController} 的签名为准。
 */
@FeignClient("third-party")
public interface ThirdPartyFeignService {

    /**
     * 查询一个行政区划到多个行政区划的直线距离。
     *
     * <p>一次传入全部终点而不是逐个调用：同一张订单的候选仓往往只有几个，
     * 批量拿回可以省掉按仓库往返的开销。
     *
     * @param fromNode 起点行政区划编码，收货地编码，不能为 {@code null}
     * @param toNodes 终点行政区划编码列表，候选仓库的编码，不能为 {@code null}
     * @return {@code data} 为终点编码到距离（单位公里）的映射；
     *         取不到坐标的终点不会出现在结果里，调用方需按缺失处理
     */
    @GetMapping("/thirdParty/front/address/distance")
    R<Map<String, BigDecimal>> getDistance(@RequestParam("fromNode") String fromNode,
                                           @RequestParam("toNodes") List<String> toNodes);
}

package thirdParty.controller;

import common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import thirdParty.service.AddressService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import thirdParty.vo.AreaTreeNode;

/**
 * 行政区划接口：省市区地址树查询与直线距离计算。
 *
 * <p>整个控制器挂在 {@code thirdParty/front} 前缀下，网关按该前缀放行匿名访问，
 * 距离接口因此同样对外可达；它只返回行政区划中心点之间的距离，不含业务数据。
 */
@RestController
@RequestMapping("thirdParty/front/address")
@Slf4j
public class AddressController {

    private final AddressService addressService;

    /**
     * 注入行政区划服务。
     *
     * @param addressService 行政区划服务，不能为 {@code null}
     */
    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    /**
     * 查询完整的地址树。
     *
     * <p>每次请求都全量读 {@code address} 表并在内存中组装，结果不缓存。
     *
     * @return 顶层节点列表，子节点按层级挂在 {@code children} 下；表中无数据时返回空列表
     */
    @RequestMapping("/tree")
    public R<List<AreaTreeNode>> getAddressTree() {
        log.info("获取地址树形结构信息");
        return R.ok(addressService.getAddressTree());
    }

    /**
     * 计算一个行政区划到多个行政区划的直线距离。
     *
     * <p>服务间接口：ware 侧算运费时起点传收货地、终点传该商品有库存行的全部仓库，
     * 一次调用拿全，避免按仓库逐个往返。
     *
     * @param fromNode 起点行政区划编码，不能为空，且必须在行政区划表中存在
     * @param toNodes 终点行政区划编码列表，逗号分隔；取不到坐标的编码不会出现在结果里
     * @return 终点编码到直线距离（单位公里，保留 3 位小数）的映射
     */
    @GetMapping("/distance")
    public R<Map<String, BigDecimal>> getDistance(@RequestParam("fromNode") String fromNode,
                                                  @RequestParam("toNodes") List<String> toNodes) {
        return R.ok(addressService.getDistanceKm(fromNode, toNodes));
    }
}

package order.feign;

import common.utils.R;
import order.vo.FareVo;
import order.vo.SkuStockVo;
import order.vo.WareSkuLockVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;


/**
 * 仓库服务的远程调用接口，用于查库存、算运费与锁定库存。
 */
@FeignClient("ware")
public interface WmsFeignService {

    /**
     * 批量查询这些 SKU 是否有库存。
     *
     * @param skuIds 待查询的 SKU 标识，不能为 {@code null}
     * @return {@code data} 为每个 SKU 的库存情况；ware 返回失败（{@code code} 非 0）时 {@code data} 为 {@code null}
     */
    @PostMapping(value = "/ware/waresku/hasStock")
    R<List<SkuStockVo>> getSkuHasStock(@RequestBody List<Long> skuIds);


    /**
     * 按收货地址查询运费与地址详情。
     *
     * @param addrId 收货地址 id，不能为 {@code null}
     * @return {@code data} 为运费与地址；ware 查不到该地址时 {@code data} 为 {@code null}
     */
    @GetMapping(value = "/ware/wareinfo/fare")
    R<FareVo> getFare(@RequestParam("addrId") Long addrId);


    /**
     * 锁定订单占用的库存。
     *
     * @param vo 锁库存请求，{@code orderSn} 与 {@code locks} 不能为空
     * @return 统一响应；{@code code} 为 0 表示锁定成功，非 0 表示库存不足
     *         （{@code NO_STOCK_EXCEPTION}）；{@code data} 无业务含义，本接口不使用
     */
    @PostMapping(value = "/ware/waresku/lock/order")
    R<Void> orderLockStock(@RequestBody WareSkuLockVo vo);
}

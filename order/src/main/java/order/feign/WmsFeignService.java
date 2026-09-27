package order.feign;

import common.utils.R;
import order.vo.SkuStockVo;
import order.vo.WareFareQueryVo;
import order.vo.WareFareVo;
import order.vo.WareSkuLockVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;


/** 仓库服务的远程调用接口，用于查库存、算运费与锁定库存。 */
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
     * 按收货地区划与商品清单计算运费。
     *
     * @param query 收货地区划编码与要计价的商品清单，不能为 {@code null}
     * @return {@code data} 为整单运费与按商品拆分的明细；商品没有库存记录、
     *         或候选仓都取不到距离时 {@code code} 非 0，{@code msg} 说明是哪一种
     */
    @PostMapping(value = "/ware/wareinfo/fare")
    R<WareFareVo> getFare(@RequestBody WareFareQueryVo query);


    /**
     * 锁定订单占用的库存。
     *
     * @param vo 锁库存请求，{@code orderSn} 与 {@code locks} 不能为空
     * @return {@code code} 为 0 表示锁定成功，非 0 表示库存不足（{@code NO_STOCK_EXCEPTION}）；
     *         ware 侧把锁定结果放在 {@code data} 里，但调用方只用 {@code code} 判断
     */
    @PostMapping(value = "/ware/waresku/lock/order")
    R<Boolean> orderLockStock(@RequestBody WareSkuLockVo vo);
}

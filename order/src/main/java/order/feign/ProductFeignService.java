package order.feign;

import common.to.CommentBriefVo;
import common.utils.R;
import order.vo.SkuInfoVo;
import order.vo.SpuInfoVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;


/**
 * 商品服务的远程调用接口，用于补齐订单项上的 SPU 与 SKU 信息。
 */
@FeignClient("product")
public interface ProductFeignService {

    /**
     * 按 SKU 查询它所属的 SPU 信息。
     *
     * @param skuId 商品 SKU 标识，不能为 {@code null}
     * @return {@code data} 为 SPU 信息；product 返回失败（{@code code} 非 0）时 {@code data} 为 {@code null}
     */
    @GetMapping(value = "/product/spuinfo/skuId/{skuId}")
    public R<SpuInfoVo> getSpuInfoBySkuId(@PathVariable("skuId") Long skuId);

    /**
     * 按 SKU 查询 SKU 自身的名称、图片与价格。
     *
     * @param skuId 商品 SKU 标识，不能为 {@code null}
     * @return {@code data} 为 SKU 信息；SKU 不存在或 product 返回失败时 {@code data} 为 {@code null}
     */
    @RequestMapping("/product/skuinfo/info/{skuId}")
    public R<SkuInfoVo> getSkuInfoBySkuId(@PathVariable("skuId") Long skuId);

    /**
     * 批量查询这些订单项里哪些已经评价过，供会员订单列表决定显示「评价」还是「查看评价」。
     *
     * <p>调用直连 product、不经网关，所以只传订单项 ID、不传会员身份 —— 这些订单项已经属于
     * 当前要展示的那张单。</p>
     *
     * @param orderItemIds 订单项 ID 列表，不能为 {@code null}；为空时返回空列表
     * @return {@code data} 为评价摘要列表；调用失败时对象为 {@code null} 或 {@code code} 非 0，
     *         调用方按"都没评价"降级
     */
    @PostMapping("/product/spucomment/commented-items")
    public R<List<CommentBriefVo>> listCommentedItems(@RequestBody List<Long> orderItemIds);

}

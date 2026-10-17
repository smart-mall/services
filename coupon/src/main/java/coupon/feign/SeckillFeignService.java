package coupon.feign;

import common.to.SeckillOfflineTo;
import common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * seckill 秒杀服务的 Feign 契约：删除场次或场次内商品时通知它把对应缓存摘掉。
 *
 * <p>缓存键名规则只属于 seckill，本服务不拼键，只把"哪些场次、哪些商品"告诉它。
 *
 * <p>返回值统一是 {@link R}：调用方必须先判 {@code code}，为 0 时才能读 {@code data}。
 */
@FeignClient("seckill")
public interface SeckillFeignService {

    /**
     * 下架指定场次或场次下的指定商品。
     *
     * @param requests 下架请求，不能为 {@code null}；{@code skuIds} 为空的项表示整个场次
     * @return 统一响应；{@code code} 为 0 时 {@code data} 是实际摘掉的商品条目数，非 0 时 {@code data} 为 {@code null}
     */
    @PostMapping("/seckill/seckillsku/offline")
    R<Integer> offline(@RequestBody List<SeckillOfflineTo> requests);
}

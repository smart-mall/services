package seckill.vo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 秒杀下单请求体。
 *
 * <p>下单会真实扣减库存，必须走 POST：GET 会被浏览器预取、被爬虫跟随、被回退重放，
 * 每次重放都是一次真实扣减。
 *
 * <p>三个值都必须由前端原样回传、不能自己拼：{@code killId} 与 {@code key} 是后端上架时写进 Redis 的，
 * 页面展示的秒杀价和真正下单用的价格都取自 Redis 里那份数据。校验见
 * {@link seckill.service.impl.SeckillServiceImpl#kill}。
 */
@Data
public class KillRequestVo {

    /** 场次 ID + "-" + SKU ID，例如 "4-45"，和 Redis hash 里的 field 一一对应。 */
    @NotBlank(message = "秒杀场次不能为空")
    private String killId;

    /** 上架时生成的随机码；只有秒杀进行中才随商品详情下发，因此它同时是“当前可抢”的凭证。 */
    @NotBlank(message = "秒杀随机码不能为空")
    private String key;

    /** 购买数量，上限是每人限购数，由后端按 Redis 里的 {@code seckillLimit} 校验。 */
    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量至少为 1")
    private Integer num;

}

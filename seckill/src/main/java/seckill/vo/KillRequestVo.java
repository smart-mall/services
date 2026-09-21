package seckill.vo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 秒杀下单的请求体。
 *
 * <p>原来这三个值都挂在 URL 查询串上（{@code /kill?killId=4-45&key=xxx&num=1}），
 * 而 {@code /kill} 是 GET —— 一个带副作用的 GET，会被浏览器预取、被爬虫跟、被回退重放，
 * 每次重放都是一次真实扣库存。改成 POST + 请求体。</p>
 *
 * <p>三个值都必须由前端原样回传、不能自己拼：{@code killId} 和 {@code key} 是后端上架时写进 Redis 的，
 * 用户在页面上看到的"秒杀价"和真正下单用的是 Redis 里那份数据，前端只有一个"凭证"的作用。
 * 具体校验在 {@link seckill.service.impl.SeckillServiceImpl#kill}。</p>
 */
@Data
public class KillRequestVo {

    /** 场次id + "-" + skuId，例如 "4-45"，和 Redis hash 里的 field 一一对应 */
    @NotBlank(message = "秒杀场次不能为空")
    private String killId;

    /** 上架时生成的随机码；只有秒杀进行中才会随商品详情下发，所以它同时是一张"当前可抢"的证明 */
    @NotBlank(message = "秒杀随机码不能为空")
    private String key;

    /** 购买数量。上限是每人限购数，由后端按 Redis 里的 seckillLimit 校验 */
    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量至少为 1")
    private Integer num;

}

package seckill.web;

import common.utils.R;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import seckill.service.SeckillService;
import seckill.vo.KillRequestVo;

/**
 * 秒杀前台接口（给 Vue 用）。
 *
 * <p>取代原来那个 {@code SeckillController}：它一个类里混了两种东西 ——
 * 两个 {@code @ResponseBody} 的 JSON 接口挂在根路径上，和一个返回 Thymeleaf 视图
 * {@code success} 的 {@code /kill}。前台换成 Vue 之后视图那条路走不通了，
 * 而且 {@code /kill} 是 GET，抢购这种带副作用的操作不该是 GET（会被预取、被回退重放）。</p>
 *
 * <p><b>路径为什么是 {@code seckill/front}</b>：网关的 seckill-api-route 是
 * {@code Path=/api/seckill/**} + {@code RewritePath=/api/(?<segment>.*),/$\{segment\}}，
 * 所以 SPA 调 {@code /api/seckill/front/current} 到服务里就是 {@code /seckill/front/current}。
 * 和 {@code product/front}、{@code search/front}、{@code order/front} 保持一致。
 * 这条路由本来就配好了，只是老 controller 把接口映射在根路径（{@code /getCurrentSeckillSkus}）上，
 * 剥掉 {@code /api} 之后变成 {@code /seckill/getCurrentSeckillSkus}，和谁都对不上，一直是 404。</p>
 *
 * <p><b>登录</b>：只有抢购需要登录，由 {@code LoginUserInterceptor} 拦 {@code /seckill/front/kill}
 * 并返回真 HTTP 401。浏览秒杀场次匿名可见 —— 首页上的秒杀栏要在没登录时就显示出来。</p>
 *
 * <p>另外 {@code GET /sku/seckill/{skuId}} <b>不在</b>这里，它是 product 通过 Feign 调的内部接口，
 * 见 {@link SeckillInternalController}。</p>
 */
@RestController
@RequestMapping("seckill/front")
public class SeckillFrontController {

    private final SeckillService seckillService;

    public SeckillFrontController(SeckillService seckillService) {
        this.seckillService = seckillService;
    }

    /**
     * 当前正在进行的秒杀场次里的商品，给首页秒杀栏和秒杀页用。
     *
     * <p>没有场次时返回空数组，不返回 null —— 前端不用先判空再遍历。
     * 返回对象里的 {@code randomCode} 是抢购要用的凭证，秒杀进行中才有值。</p>
     */
    @GetMapping("/current")
    public R current() {
        return R.ok().setData(seckillService.getCurrentSeckillSkus());
    }

    /**
     * 抢购。成功返回秒杀订单号，失败按原因返回 18xxx（已抢完 / 超限购 / 已经抢过 / 请求无效）。
     *
     * <p>成功不代表订单已经存在：订单号在这里生成后发 MQ，由 order 消费后异步建单。
     * 前端拿到订单号要轮询订单列表等它出现，再跳支付。</p>
     */
    @PostMapping("/kill")
    public R kill(@Valid @RequestBody KillRequestVo vo) throws InterruptedException {
        String orderSn = seckillService.kill(vo.getKillId(), vo.getKey(), vo.getNum());
        return R.ok().setData(orderSn);
    }
}
